
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {

  /******************************************        MODULE 1       *********************************************************************** */

  /* TODO: CRIO_TASK_MODULE_JSON_PARSING
   Task:
        - Read the json file provided in the argument[0], The file is available in the classpath.
        - Go through all of the trades in the given file,
        - Prepare the list of all symbols a portfolio has.
        - if "trades.json" has trades like
          [{ "symbol": "MSFT"}, { "symbol": "AAPL"}, { "symbol": "GOOGL"}]
          Then you should return ["MSFT", "AAPL", "GOOGL"]
   Hints:
     1. Go through two functions provided - #resolveFileFromResources() and #getObjectMapper
        Check if they are of any help to you.
     2. Return the list of all symbols in the same order as provided in json.

   Note:
   1. There can be few unused imports, you will need to fix them to make the build pass.
   2. You can use "./gradlew build" to check if your code builds successfully.
  */

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {

    // String filePath = args[0];
    
    File file = resolveFileFromResources(args[0]);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] trades = objectMapper.readValue(file, PortfolioTrade[].class);

    // Read JSON file and extract symbols
    List<String> symbols = new ArrayList<>();
    for (PortfolioTrade trade : trades) {
        symbols.add(trade.getSymbol());
    }

    return symbols;
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }


/*TODO: CRIO_TASK_MODULE_JSON_PARSING
   Follow the instructions provided in the task documentation and fill up the correct values for
   the variables provided. First value is provided for your reference.
   A. Put a breakpoint on the first line inside mainReadFile() which says
     return Collections.emptyList();
   B. Then Debug the test #mainReadFile provided in PortfoliomanagerApplicationTest.java
   following the instructions to run the test.
   Once you are able to run the test, perform following tasks and record the output as a
   String in the function below.
   Use this link to see how to evaluate expressions -
   https://code.visualstudio.com/docs/editor/debugging#_data-inspection
   1. evaluate the value of "args[0]" and set the value
      to the variable named valueOfArgument0 (This is implemented for your reference.)
   2. In the same window, evaluate the value of expression below and set it
   to resultOfResolveFilePathArgs0
      expression ==> resolveFileFromResources(args[0])
   3. In the same window, evaluate the value of expression below and set it
   to toStringOfObjectMapper.
   You might see some garbage numbers in the output. Dont worry, its expected.
     expression ==> getObjectMapper().toString()
   4. Now Go to the debug window and open stack trace. Put the name of the function you see at
   second place from top to variable functionNameFromTestFileInStackTrace
   5. In the same window, you will see the line number of the function in the stack trace window.
   assign the same to lineNumberFromTestFileInStackTrace
   Once you are done with above, just run the corresponding test and
   make sure its working as expected. use below command to do the same.
   ./gradlew test --tests PortfolioManagerApplicationTest.testDebugValues
*/

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "/home/crio-user/workspace/om-236300-ME_QMONEY_V2/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@6150c3ec";
    String functionNameFromTestFileInStackTrace = "mainReadFile(String[])";
    String lineNumberFromTestFileInStackTrace = "49";


    return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
        toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
        lineNumberFromTestFileInStackTrace});
  }











  //***********************************         MODULE 2         *********************************************************************************



  /*TODO: CRIO_TASK_MODULE_CALCULATIONS
    Now that you have the list of PortfolioTrade and their data, calculate annualized returns
    for the stocks provided in the Json.
    Use the function you just wrote #calculateAnnualizedReturns.
    Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

    Note:
    1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
    2. Remember to get the latest quotes from Tiingo API.






    TODO: CRIO_TASK_MODULE_REST_API
    Find out the closing price of each stock on the end_date and return the list
    of all symbols in ascending order by its close value on end date.

    Note:
    1. You may have to register on Tiingo to get the api_token.
    2. Look at args parameter and the module instructions carefully.
    2. You can copy relevant code from #mainReadFile to parse the Json.
    3. Use RestTemplate#getForObject in order to call the API,
      and deserialize the results in List<Candle>
  */





  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.
  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    // Extracting arguments
    String fileName = args[0];
    String endDate = args[1];
    String token = "d4ee45efc729a932be1fb37b9f7688ea2e987e2a"; 

    // Read portfolio trades from JSON file
    List<PortfolioTrade> trades = readTradesFromJson(fileName);

    // List to store closing prices of each symbol
    List<TotalReturnsDto> symbolsAndCp = new ArrayList<>();
    
    //List to store the TiingoCandles Objects :
    List<TiingoCandle> candles = new ArrayList<>();

    // Loop through each trade and fetch closing price for each symbol
    for (PortfolioTrade trade : trades) {
        // Prepare URL for fetching stock quotes
        String url = prepareUrl(trade, LocalDate.parse(endDate), token);

        // Call Tiingo API to get stock quotes
        RestTemplate restTemplate = new RestTemplate();
       
        try {
            candles = Arrays.asList(restTemplate.getForObject(url, TiingoCandle[].class));
            double closingPrice = candles.get(candles.size() - 1).getClose();
            
            TotalReturnsDto dto = new TotalReturnsDto(trade.getSymbol(), closingPrice);
            symbolsAndCp.add(dto);

        } catch (Exception e) {
            // Throw the caught exception
            throw new RuntimeException("Failed to fetch data for symbol: " + trade.getSymbol(), e);
        }
    }

    // Sort symbols based on closing prices
    List<String> sortedSymbols = sortSymbolsByClosingPrices(symbolsAndCp);        


    return sortedSymbols;
  }

  public static List<String> sortSymbolsByClosingPrices(List<TotalReturnsDto> symbolsAndCp)
  {
    Collections.sort(symbolsAndCp, new Comparator<TotalReturnsDto>() {
      @Override
      public int compare(TotalReturnsDto t1, TotalReturnsDto t2) {
        if(t1.getClosingPrice() > t2.getClosingPrice()){
          return +1;
        }else if(t1.getClosingPrice() < t2.getClosingPrice()) 
          return -1;
        
        return 0;
      }
    });

    List<String> result = new ArrayList<>();

    for(TotalReturnsDto trade : symbolsAndCp){
      result.add(trade.getSymbol());
    }

    return result;
  }



  // TODO:
  //  After refactor, make sure that the tests pass by using these two commands
  //  ./gradlew test --tests PortfolioManagerApplicationTest.readTradesFromJson
  //  ./gradlew test --tests PortfolioManagerApplicationTest.mainReadFile
  public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
    File filePath = resolveFileFromResources(filename);
    ObjectMapper objectMapper = getObjectMapper();
    List<PortfolioTrade> trades = objectMapper.readValue(filePath, new TypeReference<List<PortfolioTrade>>() {});
    return trades;
  }


  // TODO:
  //  Build the Url using given parameters and use this function in your code to call the API.
  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
    
    // Constructing the base URL
    StringBuilder urlBuilder = new StringBuilder("https://api.tiingo.com/tiingo/daily/");
    urlBuilder.append(trade.getSymbol()); // Append symbol

    // Appending parameters to the URL
    urlBuilder.append("/prices?startDate=").append(trade.getPurchaseDate());
    urlBuilder.append("&endDate=").append(endDate);
    urlBuilder.append("&token=").append(token);

    return  urlBuilder.toString();

  }

  
  
  
  
  
  
  //************************************        MODULE 3          ***************************************************************************************  
  

  
  
  /* TODO:
    Ensure all tests are passing using below command
    ./gradlew test --tests ModuleThreeRefactorTest
    
    
    
    TODO: CRIO_TASK_MODULE_CALCULATIONS
    Return the populated list of AnnualizedReturn for all stocks.
    Annualized returns should be calculated in two steps:
      1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
        1.1 Store the same as totalReturns
      2. Calculate extrapolated annualized returns by scaling the same in years span.
        The formula is:
        annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
        2.1 Store the same as annualized_returns
    Test the same using below specified command. The build should be successful.
        ./gradlew test --tests PortfolioManagerApplicationTest.testCalculateAnnualizedReturn
  */
  
  public static String getToken(){
    return "d4ee45efc729a932be1fb37b9f7688ea2e987e2a";
  }
  
  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    if (candles.isEmpty()) {
      return 0.0;
    }
    return candles.get(0).getOpen();
  }

  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
     if(candles.isEmpty()){
      return 0.0;
     }
     return candles.get(candles.size()-1).getClose();
  }

  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {

     List<Candle> candles = new ArrayList<>();

     //Make url
     String url = prepareUrl(trade, endDate, token);

     RestTemplate restTemplate = new RestTemplate();

     candles = Arrays.asList(restTemplate.getForObject(url, TiingoCandle[].class));
    
     return candles;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) 
  {
      String symbol = trade.getSymbol();
      Double totalReturns = calculateTotalReturns(buyPrice, sellPrice);
      LocalDate purchaseDate = trade.getPurchaseDate();

      // and convert it to a decimal value
      Period period = Period.between(purchaseDate, endDate);
      Double totalNumYears = period.getYears() + (double) period.getMonths() / 12 + (double) period.getDays() / 365;

      // Calculate annualized returns using the provided formula
      Double annualizedReturn = Math.pow(1 + totalReturns, 1 / totalNumYears) - 1;

      
      return new AnnualizedReturn(symbol, annualizedReturn, totalReturns);
  }

  //Calculate totalReturns of a stock :
  public static Double calculateTotalReturns(Double buyPrice, Double sellPrice){
    return (sellPrice - buyPrice)/buyPrice;
  } 

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args) throws IOException, URISyntaxException     
  {
      //Extracting parameters :
      String fileName = args[0];
      String endDate = args[1];
      String token  =  getToken();

      List<PortfolioTrade> trades = readTradesFromJson(fileName);

      List<Candle> candles;

      List<AnnualizedReturn> result = new ArrayList<>();

      for(PortfolioTrade trade : trades){
          candles = fetchCandles(trade, LocalDate.parse(endDate), token);

          Double buyPrice  = getOpeningPriceOnStartDate(candles); 
          Double sellPrice = getClosingPriceOnEndDate(candles);

          AnnualizedReturn annualReturns = calculateAnnualizedReturns(LocalDate.parse(endDate), trade, buyPrice, sellPrice);

          result.add(annualReturns);
      }

      // Sort the result list in descending order of annualizedReturn
      Collections.sort(result, Comparator.comparingDouble(AnnualizedReturn::getAnnualizedReturn).reversed());


      
      return result;
  }

  
  






//***********************************     MODULE 4 *************************************************************************************************************************************************











  /*  TODO: CRIO_TASK_MODULE_REFACTOR
    Once you are done with the implementation inside PortfolioManagerImpl and
    PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
    Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
    call the newly implemented method in PortfolioManager to calculate the annualized returns.
  */
    // Note:
    // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  private static String readFileAsString(String file) {
    try {
        // Read the contents of the file into a string
        byte[] encoded = Files.readAllBytes(Paths.get(file));
        return new String(encoded);
    } catch (IOException e) {
        e.printStackTrace();
        return null; // Return null if an exception occurs
    }
  }
  
  // Method to parse JSON content(String) into a list of PortfolioTrade objects
  private static List<PortfolioTrade> parseJsonToPortfolioTrades(String jsonContent) throws IOException {
    ObjectMapper objectMapper = getObjectMapper();
    List<PortfolioTrade> portfolioTrades = Arrays.asList(objectMapper.readValue(jsonContent, PortfolioTrade[].class));

    return portfolioTrades;
}

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
      String file = args[0];
      LocalDate endDate = LocalDate.parse(args[1]);

      // Read the contents of the JSON file
      String contents = readFileAsString(file);

      // Parsing JSON content into a list of PortfolioTrade objects
      List<PortfolioTrade> portfolioTrades = parseJsonToPortfolioTrades(contents);

      // Creating an instance of PortfolioManager using PortfolioManagerFactory
      RestTemplate restTemplate = new RestTemplate();
      PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);

      // Calculating annualized returns using PortfolioManager
      return portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);
  }

  


  




  //***********************************        MAIN METHOD     ******************************************************************************



  

  










  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    

    //Calling the functions :
    printJsonObject(mainReadFile(args));

    printJsonObject(mainReadQuotes(args));
    
    printJsonObject(mainCalculateSingleReturn(args));

    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }
}



  


