/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

/**
 * It is painful to covert ICICI's tax statement to HNR format. Entering every detail in their
 * excel sheet is painful. All they need is an XML file that needs to be uploaded and the
 * conversion happens via that xls. This program takes ICICI input and coverts it to the XML that
 * HNR expects.
 * <p>
 * E.g java -cp .: ICICI_To_HNR /Users/123/Downloads/CapitalGain_2016-2017.csv BA123XFQ 2017-18  /Users/123/Downloads/hnr_2017_18.xml
 */
public class ICICI_To_HNR {

  static class ICICIRecord {
    String stock;
    String qty;
    String saleDate;
    int saleRate;
    int saleValue;
    int saleExp;
    String purchaseDate;
    int purchaseRate;
    int purchaseValue;
    int purchaseExpense;
    int purchaseIndexCost;
    String profitLosss;
  }

  private static final List<ICICIRecord> dataList = new LinkedList();

  public static void populateData(File file) throws IOException, ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
    SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy");

    String line = "";
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      reader.readLine();  //skip first line; header
      while (reader.ready()) {
        line = reader.readLine().trim();
        if (line.startsWith("Stock")) {
          continue;
        }
        String[] data = line.split(",");

        if (data.length != 12) {
          System.out.println("Skipping " + line);
          continue;
        }
        ICICIRecord record = new ICICIRecord();

        record.stock = data[0];
        if (record.stock.isEmpty()) {
          System.out.println("Skipping " + line);
          continue;
        }

        record.qty = data[1];
        record.saleDate = sdf2.format(sdf.parse(data[2]));
        // Assuming atleast common man does not trade beyond Integer.MAX
        record.saleRate = ((int) Float.parseFloat(data[3]));
        record.saleValue = ((int) Float.parseFloat(data[4]));
        record.saleExp = ((int) Float.parseFloat(data[5]));
        record.purchaseDate = sdf2.format(sdf.parse(data[6]));
        record.purchaseRate = ((int) Float.parseFloat(data[7]));
        record.purchaseValue = ((int) Float.parseFloat(data[8]));
        record.purchaseExpense = ((int) Float.parseFloat(data[9]));
        record.purchaseIndexCost = ((int) Float.parseFloat(data[10]));
        record.profitLosss = data[11];

        dataList.add(record);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(line);
    }
  }

  private static Element createElement(String name, String value, Document doc) {
    Element node = doc.createElement(name);
    if (node != null) {
      node.setTextContent(value);
    }
    return node;
  }

  /**
   * Generate XML given PAN, assessment year and destination file
   * which can be directly uploaded to HnR block
   *
   * @param pan
   * @param year
   * @param dest
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws TransformerException
   */
  public static void generateXML(String pan, String year, File dest)
      throws IOException, ParserConfigurationException, TransformerException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

    Document doc = docBuilder.newDocument();
    Element cgDetails = createElement("CG_Details", null, doc);
    doc.appendChild(cgDetails);

    // PAN

    Element panElement = createElement("PAN", pan, doc);
    cgDetails.appendChild(panElement);

    // AY
    Element assessmentYear = createElement("AY", year, doc);
    cgDetails.appendChild(assessmentYear);

    // inINR
    Element inINR = createElement("In_INR", null, doc);
    cgDetails.appendChild(inINR);

    // cgINR
    Element cgINR = createElement("cg_INR", null, doc);
    cgDetails.appendChild(cgINR);

    //Records
    for (ICICIRecord record : dataList) {
      //CG
      Element cg = createElement("CG", null, doc);

      cg.appendChild(createElement("Type",
          "Equity shares in listed companies in India - equity oriented mutual funds(listed - unlisted)in India",
          doc));
      cg.appendChild(createElement("Particulars", record.stock, doc));
      cg.appendChild(createElement("DateOfSale", record.saleValue + "", doc));
      cg.appendChild(createElement("SaleValue", record.saleValue + "", doc));
      //Yes, spelling mistake from HNR block
      cg.appendChild(createElement("SaleExpences", record.saleExp + "", doc));
      cg.appendChild(createElement("DateOfPurchase", record.purchaseDate, doc));
      cg.appendChild(createElement("PurchaseCost", record.purchaseIndexCost + "", doc));
      cg.appendChild(createElement("PurchaseExpenses", record.purchaseExpense + "", doc));
      cg.appendChild(createElement("STT_Paid", "Yes", doc));

      cgDetails.appendChild(cg);
    }

    // write the content into xml file
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(new StringWriter());

    transformer.transform(source, result);
    String xmlString = result.getWriter().toString();
    FileWriter writer = new FileWriter(dest);
    writer.write(xmlString);
    writer.close();
    System.out.println("File saved!.." + dest.getAbsolutePath());
  }

  public static void main(String[] args) throws Exception {
    //ICICI csv file, pan, year (e.g 2017-18), destinationFile
    if (args.length != 4) {
      throw new IllegalArgumentException(
          "Please enter icici csv file, pan, year (e.g 2017-18), destination file");
    }
    populateData(new File(args[0]));
    generateXML(args[1], args[2], new File(args[3]));
  }
}
