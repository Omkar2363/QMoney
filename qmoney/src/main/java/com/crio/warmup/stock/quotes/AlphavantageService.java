
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.AlphaVantageRateLimitException;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService 
{

  private final RestTemplate restTemplate;
  private final String APIKEY = "JFP4CQUHTD7I10YF";

  AlphavantageService(RestTemplate restTemplate){
    this.restTemplate = restTemplate;
  }


  /*TODO : CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
    Implement the StockQuoteService interface as per the contracts. Call Alphavantage service
    to fetch daily adjusted data for last 20 years.
    Refer to documentation here: https://www.alphavantage.co/documentation/
    --
    The implementation of this functions will be doing following tasks:
      1. Build the appropriate url to communicate with third-party.
          The url should consider startDate and endDate if it is supported by the provider.
      2. Perform third-party communication with the url prepared in step#1
      3. Map the response and convert the same to List<Candle>
      4. If the provider does not support startDate and endDate, then the implementation
          should also filter the dates based on startDate and endDate. Make sure that
          result contains the records for for startDate and endDate after filtering.
      5. Return a sorted List<Candle> sorted ascending based on Candle#getDate
    IMP: Do remember to write readable and maintainable code, There will be few functions like
      Checking if given date falls within provided date range, etc.
      Make sure that you write Unit tests for all such functions.
    Note:
    1. Make sure you use {RestTemplate#getForObject(URI, String)} else the test will fail.
    2. Run the tests using command below and make sure it passes:
      ./gradlew test --tests AlphavantageServiceTest
    CHECKSTYLE : OFF
      
    CHECKSTYLE : ON
    TODO : CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
    1. Write a method to create appropriate url to call Alphavantage service. The method should
        be using configurations provided in the {@link @application.properties}.
    2. Use this method in #getStockQuote.
  */

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
          throws JsonProcessingException, StockQuoteServiceException {
    try{
        String url = buildUri(symbol);
        
        // Fetching the JSON response from the API
        String alphavantageResponse = restTemplate.getForObject(url, String.class);

        // Parsing the JSON response to AlphavantageDailyResponse object
        AlphavantageDailyResponse response = parseResponse(alphavantageResponse);

        List<Candle> candles = parseAndSortCandles(response, from, to);
        
        return candles;
    } catch (HttpClientErrorException e) {
        if (e.getStatusCode().value() == 429) {
            throw new AlphaVantageRateLimitException("Alpha Vantage rate limit exceeded", e);
        } else {
            throw new StockQuoteServiceException("Error accessing Alpha Vantage", e);
        }
    }catch (JsonProcessingException e) {
        throw new StockQuoteServiceException("Error processing JSON response", e);
    } catch (Exception e) {
        throw new StockQuoteServiceException("Error retrieving stock quote", e);
    }
  }

  private List<Candle> parseAndSortCandles(AlphavantageDailyResponse response, LocalDate from, LocalDate to) throws StockQuoteServiceException{
      List<Candle> candles = new ArrayList<>();

      try{
        Map<LocalDate, AlphavantageCandle> candlesMap = response.getCandles();
        for(LocalDate date : response.getCandles().keySet()){
          if(date.compareTo(from) >= 0  &&  date.compareTo(to) <= 0) {
              AlphavantageCandle candle = candlesMap.get(date);
              candle.setDate(date);
              candles.add(candle);
          }
        }
      }catch (Exception e) {
          throw new StockQuoteServiceException("Error retrieving stock quote", e);
      }
      

      Collections.sort(candles, getComparator());

      return candles;

  }

  private Comparator<Candle> getComparator(){
    return Comparator.comparing(Candle::getDate);
  }

  protected String buildUri(String symbol) 
  {
    StringBuilder urlBuilder = new StringBuilder("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY");
    urlBuilder.append("&symbol=").append(symbol);
    urlBuilder.append("&outputsize=full"); 
    urlBuilder.append("&apikey=").append(APIKEY);
    return urlBuilder.toString();
  } 

  private AlphavantageDailyResponse parseResponse(String alphavantageResponse) throws JsonProcessingException 
  { 
    ObjectMapper objectMapper = getObjectMapper();
    AlphavantageDailyResponse candles = objectMapper.readValue(alphavantageResponse, AlphavantageDailyResponse.class);

    return candles;
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
  
  
}

//Link of alphavantage API :
//https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=aapl&outputsize=full&apikey=JFP4CQUHTD7I10YF
