
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.AlphaVantageRateLimitException;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {


  private final RestTemplate restTemplate;
  private final String token = "d4ee45efc729a932be1fb37b9f7688ea2e987e2a";

  
  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  /*TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
    Implement getStockQuote method below that was also declared in the interface.

    Note:
    1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
    2. Run the tests using command below and make sure it passes.
      ./gradlew test --tests TiingoServiceTest
  */


  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException, StockQuoteServiceException {
    // TODO Auto-generated method stub
    try{
      String url = buildUri(symbol, from, to);
      
      //Getting the String format
      String tiingoResponse = restTemplate.getForObject(url, String.class);
      ObjectMapper objectMapper = getObjectMapper();
      TiingoCandle[] tiingoCandles = objectMapper.readValue(tiingoResponse, TiingoCandle[].class);
      List<Candle> candles = Arrays.asList(tiingoCandles);
      
      candles.sort(Comparator.comparing(Candle::getDate));
      
      return candles;
    }catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 429) {
          throw new AlphaVantageRateLimitException("Alpha Vantage rate limit exceeded", e);
      } else {
          throw new StockQuoteServiceException("Error accessing Alpha Vantage", e);
      }
    }
    catch (Exception e) {
      throw new StockQuoteServiceException("Error retrieving stock quote from Tiingo", e);
    }
  }
  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.
  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) 
  {  
    StringBuilder urlBuilder = new StringBuilder("https://api.tiingo.com/tiingo/daily/");
    urlBuilder.append(symbol); // Append symbol

    // Appending parameters to the URL
    urlBuilder.append("/prices?startDate=").append(startDate);
    urlBuilder.append("&endDate=").append(endDate);
    urlBuilder.append("&token=").append(token);

    return  urlBuilder.toString();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }


}
