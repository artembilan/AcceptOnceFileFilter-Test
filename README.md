# AcceptOnceFileFilter-Test
Code is used to test a question in Stack Exchange.

This is a Spring Boot application built with Maven. Build it as a jar package. 

You will need to modify the batchload-dev.properties file with your directory settings. 

Line 74 in BatchIntegrationConfiguration is the issue. Commented out the program polls the directory and when the file in question is found, it operates on it. When uncommented the file will never be picked up.

I did notice that if the file is there upon startup, it will act upon in with the line uncommented.

I used touch foobar_CLAIM_1.txt to create a file. It will take about 15 seconds to pick it up.
