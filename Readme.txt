During tax filing times, it is painful to download data from ICICI and enter into HNR block's excel sheet just to create the xml file that is needed by them.

This tool, takes data from ICICI (csv file) and creates the XML file which can be uploaded to HNR.

1. mvn clean package
2. Download ICICI data from the bank.
3. java -cp ./target/*: ICICI_To_HNR CapitalGain_2016-2017.csv BA123XFQ 2017-18 sample_output.xml
