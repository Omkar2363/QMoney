
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private final RestTemplate restTemplate;
  private final StockQuotesService stockQuotesService;
  private final String token = "d4ee45efc729a932be1fb37b9f7688ea2e987e2a";


  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    this.stockQuotesService = null;
  }

  protected PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
    this.restTemplate = null;
  }
  
  

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException, StockQuoteServiceException 
  {
    return stockQuotesService.getStockQuote(symbol, from, to);
  }

  /*
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException 
  {
     String uri = buildUri(symbol, from, to);
     List<Candle> candles = Arrays.asList(restTemplate.getForObject(uri, TiingoCandle[].class));

     return candles;
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    StringBuilder urlBuilder = new StringBuilder("https://api.tiingo.com/tiingo/daily/");
    urlBuilder.append(symbol); // Append symbol

    // Appending parameters to the URL
    urlBuilder.append("/prices?startDate=").append(startDate);
    urlBuilder.append("&endDate=").append(endDate);
    urlBuilder.append("&token=").append(token);

    return  urlBuilder.toString();
  }
  */ 

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) throws StockQuoteServiceException {
    // TODO Auto-generated method stub
    
    List<AnnualizedReturn> result = new ArrayList<>();

    for(PortfolioTrade trade : portfolioTrades){
      try {
        List<Candle> candles = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
      
        Double buyPrice  = getOpeningPriceOnStartDate(candles);
        Double sellPrice = getClosingPriceOnEndDate(candles);
  
        AnnualizedReturn annualizedReturns = calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice);
      
        result.add(annualizedReturns);
        } catch (JsonProcessingException e) {
          // Handling the exception
          e.printStackTrace(); 
        }
      }
    
    result.sort(getComparator());
    
    return result;
  }


  //Extra Methods Defination :
  private Double getOpeningPriceOnStartDate(List<Candle> candles){
    if(candles.isEmpty()){
      return 0.0;
    }
    return candles.get(0).getOpen();
  }

  private Double getClosingPriceOnEndDate(List<Candle> candles){
    if(candles.isEmpty()){
      return 0.0;
    }
    return candles.get(candles.size()-1).getClose();
  }

  private Double calculateTotalReturns(Double buyPrice, Double sellPrice){
    return (sellPrice - buyPrice)/buyPrice;
  }

  private AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade, Double buyPrice, Double sellPrice)
  {
    String symbol = trade.getSymbol();
    LocalDate purchaseDate = trade.getPurchaseDate();
    Double totalDays = (double)DAYS.between(purchaseDate, endDate);
    Double totalNumYears = totalDays/365;

    Double totalReturns = calculateTotalReturns(buyPrice, sellPrice);
    
    Double annualizedReturn = Math.pow(1 + totalReturns, 1 / totalNumYears) - 1;


    return new AnnualizedReturn(symbol, annualizedReturn, totalReturns);
  }

  


  /*TODO: CRIO_TASK_MODULE_REFACTOR
    1. Now we want to convert our code into a module, so we will not call it from main anymore.
      Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
      into #calculateAnnualizedReturn function here and ensure it follows the method signature.
    2. Logic to read Json file and convert them into Objects will not be required further as our
      clients will take care of it, going forward.

    Note:
    Make sure to exercise the tests inside PortfolioManagerTest using command below:
    ./gradlew test --tests PortfolioManagerTest

    CHECKSTYLE:OFF





    TODO: CRIO_TASK_MODULE_REFACTOR
    Extract the logic to call Tiingo third-party APIs to a separate function.
    Remember to fill out the buildUri function and use that.
  */


  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Modify the function #getStockQuote and start delegating to calls to
  //  stockQuoteService provided via newly added constructor of the class.
  //  You also have a liberty to completely get rid of that function itself, however, make sure
  //  that you do not delete the #getStockQuote function.





  //Implementing the Multithreading to reduce the latency.......
  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades,
      LocalDate endDate, int numThreads) throws InterruptedException,
      StockQuoteServiceException {

      List<Callable<AnnualizedReturn>> tasks = createTasks(portfolioTrades, endDate);
      List<AnnualizedReturn> result = executeTasks(tasks, numThreads);
      result.sort(getComparator());
      return result;
  }

  private List<Callable<AnnualizedReturn>> createTasks(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate) {
      
      return portfolioTrades.stream()
          .map(trade -> createTask(trade, endDate))
          .collect(Collectors.toList());
  }

  private Callable<AnnualizedReturn> createTask(
      PortfolioTrade trade, LocalDate endDate) {
      
      return () -> {
          try {
              List<Candle> candles = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
              Double buyPrice = getOpeningPriceOnStartDate(candles);
              Double sellPrice = getClosingPriceOnEndDate(candles);
              return calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice);
          } catch (JsonProcessingException e) {
              throw new StockQuoteServiceException("Error fetching stock quote for symbol: " + trade.getSymbol(), e);
          } catch (Exception e) {
              throw new StockQuoteServiceException("Error processing portfolio trade", e);
          }
      };
  }

  private List<AnnualizedReturn> executeTasks(
      List<Callable<AnnualizedReturn>> tasks, int numThreads) throws InterruptedException,
      StockQuoteServiceException {

      ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
      List<Future<AnnualizedReturn>> futures = executorService.invokeAll(tasks);
      executorService.shutdown();
      executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

      List<AnnualizedReturn> result = new ArrayList<>();
      for (Future<AnnualizedReturn> future : futures) {
          try {
              result.add(future.get());
          } catch (ExecutionException e) {
              Throwable cause = e.getCause();
              if (cause instanceof StockQuoteServiceException) {
                  throw (StockQuoteServiceException) cause;
              } else {
                  throw new StockQuoteServiceException("Error occurred while processing portfolio", cause);
              }
          }
      }
      return result;
  }

  /*
    //Non-Modular Code :
    @Override
    public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
        List<PortfolioTrade> portfolioTrades,
        LocalDate endDate, int numThreads) throws InterruptedException,
        StockQuoteServiceException {

        List<AnnualizedReturn> result = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        List<Callable<AnnualizedReturn>> tasks = portfolioTrades.stream()
          .map(trade -> (Callable<AnnualizedReturn>) () -> {
              try {
                  List<Candle> candles = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
                  Double buyPrice = getOpeningPriceOnStartDate(candles);
                  Double sellPrice = getClosingPriceOnEndDate(candles);
                  return calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice);
              } catch (JsonProcessingException e) {
                  throw new StockQuoteServiceException("Error fetching stock quote for symbol: " + trade.getSymbol(), e);
              } catch (Exception e) {
                  throw new StockQuoteServiceException("Error processing portfolio trade", e);
              }
          })
          .collect(Collectors.toList());

        List<Future<AnnualizedReturn>> futures = executorService.invokeAll(tasks);

        for (Future<AnnualizedReturn> future : futures) {
            try {
                result.add(future.get());
            } catch (ExecutionException e) {
                // Unwrap the exception thrown by the task
                Throwable cause = e.getCause();
                if (cause instanceof StockQuoteServiceException) {
                    throw (StockQuoteServiceException) cause;
                } else {
                    throw new StockQuoteServiceException("Error occurred while processing portfolio", cause);
                }
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        result.sort(getComparator());

        return result;
    } 

  */


}