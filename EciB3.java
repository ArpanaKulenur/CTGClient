
/*
*         File Name     : EciB3.java
*
*         Product       : CICS Transaction Gateway
*
*         Description   : This sample shows the basic use of the Channels and
*                         Containers components of the CICS Transaction
*                         Gateway API. When using remote mode, the sample
*                         connects to a Gateway daemon and obtains a list of
*                         available CICS servers. It then flows an ECI request
*                         for CICS program EC03 to the chosen server. When
*                         using local mode, the sample prompts for the URL of
*                         a CICS TCPIPSERVICE listening for IPIC requests,
*                         before flowing an ECI request for CICS program EC03
*                         to that server. This local mode URL is of the form
*                         "protocol://hostname:port", where protocol is "tcp"
*                         or "ssl".
*                         
*                         Note that channel and container names are case
*                         sensitive.
*
*         Pre-Requisites: Use a version of the JDK that the CICS Transaction
*                         Gateway supports if you recompile this sample. See
*                         the product documentation for supported Java levels.
*                         
*                         Use CICS Transaction Gateway version 7.1 or higher
*                         and CICS TS 3.2 or higher with IPIC enabled and the
*                         supplied CICS program EC03 installed.
*
* Licensed Materials - Property of IBM  
*  
* 5724-I81,5725-B65,5655-Y20 
*  
* (C) Copyright IBM Corp. 2001, 2020 All Rights Reserved.  
*  
* US Government Users Restricted Rights - Use, duplication or  
* disclosure restricted by GSA ADP Schedule Contract with  
* IBM Corp.  
* 
* Status: Version 9 Release 3 
*
*         The following code is sample code created by IBM Corporation.  This
*         sample code is not part of any standard IBM product and is provided
*         to you solely for the purpose of assisting you in the development of
*         your applications.  The code is provided 'AS IS', without warranty
*         or condition of any kind.  IBM shall not be liable for any damages
*         arising out of your use of the sample code, even if IBM has been
*         advised of the possibility of such damages.
*/

package com.ibm.ctg.samples.eci;

import java.io.*;
import java.util.Properties;
import com.ibm.ctg.client.*;


/**
 * Sample program to demonstrate usage of channels and containers in an ECI
 * request
 */
public class EciB3
{
static final String copyright_notice ="Licensed Materials - Property of IBM 5724-I81,5725-B65,5655-Y20 (c) Copyright IBM Corp. 2001, 2020 All Rights Reserved. US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp."; 
    
    
    //The maximum number of servers that will be listed
    //Increase this constant if you have a larger number
    //of servers defined in your CTG.INI file
    private static final int    MAX_SERVER_COUNT = 40;
    
    //Maximum length of a container name - used for formatting
    private static final int    MAX_CONTAINER_LEN = 16;
    
    //Maximum number of flow attempts
    private static final int    MAX_FLOW_ATTEMPTS = 3;
    
    //Name of the CICS program to run
    //Modify this constant to match an installed CICS program
    private static final String PROGRAM_NAME = "EC03";
    
    //Name of the channel to send to the CICS program
    //Channel and container names are case sensitive
    private static final String CHANNEL_NAME = "SAMPLECHANNEL";
    
    //Name of the CHAR container to send data in
    //Channel and container names are case sensitive
    private static final String CONTAINER_NAME = "INPUTDATA";
    

    private String         sslKeyring;
    private String         sslPassword;
    private String         gatewayUrl = "local:";
    private int            gatewayPort = 2006;
    private JavaGateway    gateway;
    private ECIRequest     eciReq;
    private BufferedReader reader;
    
    
    /**
     * Program entry point
     * 
     * @param args  command line arguments passed to the program
     */
    public static void main(String[] args)
    {
        EciB3  demoApp = new EciB3();
        
        if (demoApp.processArgs(args)) {
            demoApp.start();
        }
    }
    
    
    /**
     * Displays sample usage information and processes any commandline
     * parameters to obtain:
     *      > Gateway URL
     *      > Gateway port
     *      > SSL keyring and password 
     *      
     * @param args  command line arguments passed to the program
     */
    public boolean processArgs(String[] args)
    {
        //Display program banner
        System.out.println("CICS Transaction Gateway Basic ECI Sample 3");
        System.out.println();
        
        //Display program usage information
        System.out.println("Usage: java com.ibm.ctg.samples.eci.EciB3 [Gateway URL]");
        System.out.println("                                          [Gateway port number]");
        System.out.println("                                          [SSL keyring");
        System.out.println("                                           SSL password]");
        System.out.println();
        
        //Display trace option
        System.out.println("To enable client tracing, run the sample with the following Java option:");
        System.out.println("\t-Dgateway.T.trace=on");
        System.out.println();
        
        //Process commandline arguments
        switch (args.length) {
        case 4:
            //SSL keyring and password specified
            sslKeyring = args[2];
            sslPassword = args[3];
            
        case 2:
            //Gateway port number specified
            try {
                gatewayPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number");
                return false;
            }
            
        case 1:
            //Gateway URL specified
            gatewayUrl = args[0];
            
        case 0:
            break;
            
        case 3:
            System.out.println("An SSL password must be specified if " +
                               "using an SSL keyring.");
            return false;
            
        default:
            for (int i = 4; i < args.length; i++) {
               System.out.println("Error - unrecognised argument: " + args[i]);
            }
            return false;   
        }
        
        //Display gateway URL and port
        System.out.println("The address of the gateway has been set to " +
                           gatewayUrl + " port " + gatewayPort);
                
        return true;
    }
    
    /**
     * Connects to the gateway, flows an ECI request, and displays any output
     */
    public void start()
    {
        String  cicsServ;
        String  inputData;
        Channel reqChannel;
        Channel respChannel;
        
        try {
            //Create a BufferedReader to read user input from stdin
            reader = new BufferedReader(new InputStreamReader(System.in));
        
            if ((sslKeyring != null) && (sslPassword != null)) {
                //Create a gateway with SSL
                Properties  sslProps = new Properties();
                sslProps.setProperty(JavaGateway.SSL_PROP_KEYRING_CLASS, sslKeyring);
                sslProps.setProperty(JavaGateway.SSL_PROP_KEYRING_PW, sslPassword);
                
                gateway = new JavaGateway(gatewayUrl, gatewayPort, sslProps);
                
            } else {
                //Create a gateway without SSL
                gateway = new JavaGateway(gatewayUrl, gatewayPort);
                
            }
            
            
            if (gatewayUrl.toLowerCase().startsWith("local:")) {
                //IPIC servers are not listed by listSystems in local mode, so
                //prompt the user to input a server URL and port instead
                cicsServ = inputServer();
            } else {
                //Use listSystems to enumerate servers and prompt user to
                //select one
                cicsServ = selectServer();
            }
                
            if (cicsServ == null) {
                //No CICS server was selected, so exit
                return;
            }
            
            //Prompt for input text
            inputData = inputText();
                      
            
            //Create a channel for the ECI request and create a container on
            //the channel
            reqChannel = new Channel(CHANNEL_NAME);
            reqChannel.createContainer(CONTAINER_NAME, inputData);
            
            
            //Create the ECI request using the previously created channel -
            //if CICS logon credentials are required they are prompted for in
            //the attemptFlow method.          
            eciReq = new ECIRequest(
                        ECIRequest.ECI_SYNC,        //ECI call type
                        cicsServ,                   //CICS server
                        null,                       //CICS username
                        null,                       //CICS password
                        PROGRAM_NAME,               //Program to run
                        null,                       //Transaction to run
                        reqChannel,                 //Channel
                        ECIRequest.ECI_NO_EXTEND,   //ECI extend mode
                        0                           //ECI LUW token
                     );
                        
            
            //Attempt to flow the ECI request
            if (attemptFlow(eciReq)) {
                
                //Check the program returned a channel
                if (eciReq.hasChannel()) {
                    
                    respChannel = eciReq.getChannel();
                
                    //Display the channel name and a list of containers returned
                    //by the ECI request
                    System.out.print("Program " + PROGRAM_NAME + " returned " + respChannel.getContainers().size());
                    System.out.println(" containers in channel \"" + respChannel.getName() + "\":");
                                    
                    for (Container cont : eciReq.getChannel().getContainers()) {
                        
                        if (cont.getType() == Container.ContainerType.CHAR) {
                            //Display contents of a CHAR container
                            System.out.print("\t[CHAR] " + padString(cont.getName(), MAX_CONTAINER_LEN) + " = ");
                            System.out.println(cont.getCHARData());
                            
                        } else {
                            //Display contents of a BIT container
                            System.out.print("\t [BIT] " + padString(cont.getName(), MAX_CONTAINER_LEN) + " = ");
                            
                            byte[]  data = cont.getBITData();
                            
                            //Display as hex
                            for (int i = 0; i < data.length; i++) {
                              String   hex = "0" + Integer.toHexString(data[i]);
                              System.out.print(hex.substring(hex.length() - 2));
                            }
                            
                            System.out.println();               
                        }
                     
                    }

                } else {
                    System.out.println("Program " + PROGRAM_NAME + " did not return a channel");
                    
                }
                    
            }
            
        } catch (Exception e) {
            e.printStackTrace();
                      
        } finally {
            //Always close the gateway if open
            try {
                if ((gateway != null) && gateway.isOpen()) {
                    gateway.close();
                }
                
                //Close the BufferedReader
                reader.close();
            } catch (IOException e) {
               e.printStackTrace();
            }

        }
    }
    
    /**
     * Pads a string with trailing spaces until it is a given length
     * 
     * @param text      text to pad with spaces
     * @param length    desired length of padded string
     */
    private String padString(String text, int length)
    {
        while (text.length() < length) {
            text = text + " ";
        }
        
        return text;
    }
    
    /**
     * Attempts to flow the given ECI request, prompting the user to enter
     * CICS login credentials if required, up to a maximum of MAX_FLOW_ATTEMPTS
     * attempts
     * 
     * @param eciReq    the ECIRequest object to flow to the server
     */
    private boolean attemptFlow(ECIRequest eciReq)
    {
        int flowRc;
        int attempts = 0;
        
        while (attempts <= MAX_FLOW_ATTEMPTS) {

        	try {
        		//Attempt flow
        		gateway.flow(eciReq);
        		flowRc = eciReq.getRc();

        	} catch (IOException e) {
        		e.printStackTrace();
        		return false;
        	}

        	System.out.println();

        	switch (flowRc) {
        	case ECIRequest.ECI_NO_ERROR:

        		//No error
        		return true;

        	case ECIRequest.ECI_ERR_SECURITY_ERROR:

        		if (attempts > 0) {
        			System.out.println("Validation failed, enter your credentials again");
        			System.out.println();
        		}

        		if(attempts == MAX_FLOW_ATTEMPTS) {
        			System.out.println("Maximum attempts reached!");
        			return false;
        		}
        		//Prompt for login credentials
        		try {
        			System.out.println("Enter your CICS user ID: ");
        			eciReq.Userid = reader.readLine().trim();
        			System.out.println("Enter your CICS password or password phrase: ");
        			eciReq.Password = reader.readLine().trim();
        		} catch (IOException e) {
        			return false;
        		}
        		break;

        	case ECIRequest.ECI_ERR_TRANSACTION_ABEND:

        		//Transaction abended
        		System.out.println("The transaction abended: " + eciReq.Abend_Code);
        		return false;

        	case ECIRequest.ECI_ERR_NO_CICS:

        		//CICS not found
        		System.out.println("CICS server not found");
        		return false;

        	default:
        		System.out.println("ECI request return code: " + eciReq.getRcString());
        		return false;

        	}

        	attempts++;
        }
        
        System.out.println("Authentication failed");        
        return false;
    }

    
    /**
     * Prompts the user to enter the URL and port of a CICS server which the
     * ECI request will be sent to - used in local mode
     */
    private String inputServer() throws IOException
    {
        String cicsUrl;
        
        System.out.println();
        System.out.println("IPIC servers are not listed when running in local mode.");
        
        //Prompt for URL
        System.out.print("Enter URL of a CICS server, or Q to quit: ");
        cicsUrl = reader.readLine().trim();
        
        if (cicsUrl.equalsIgnoreCase("Q")) {
            return null;
        } else {
            return cicsUrl;
        }
    }
    
    
    /**
     * Queries the Gateway daemon for a list of CICS servers then prompts
     * the user to select a server from the list - used in remote mode
     */
    private String selectServer() throws IOException
    {
        String choice;
        int    serverNum;
        String serverName = null;
        
        //Query the Gateway daemon for a list of defined CICS servers
        eciReq = ECIRequest.listSystems(MAX_SERVER_COUNT);
        gateway.flow(eciReq);
        
        if (eciReq.getRc() != ECIRequest.ECI_NO_ERROR) {
            System.out.println("The request to list the defined CICS servers failed with " + eciReq.getRcString());
            return null;
        }
        
        if (eciReq.numServersReturned == 0) {
            System.out.println("No CICS servers have been defined");
            return null;
        }
        
        while (serverName == null) {
            
            //Display CICS server list
            System.out.println("CICS servers defined:");
            
            for (int i = 0; i < eciReq.numServersReturned; i++) {
                
                if (i < 9) {
                    System.out.print("\t " + (i + 1) + ". ");
                } else {
                    System.out.print("\t" + (i + 1) + ". ");
                }
                
                //Print the name and description of the CICS server
                //as contained in the SystemList vector
                System.out.println(eciReq.SystemList.elementAt(i * 2) + " -" +
                                   eciReq.SystemList.elementAt((i * 2) + 1));
            }
            
            //Prompt for user selection
            System.out.println();
            System.out.print("Choose server to connect to, or Q to quit: ");
            
            choice = reader.readLine();
            
            if (choice.equalsIgnoreCase("Q")) {
                return null;
            }
            
            //Parse selection
            try {
                serverNum = Integer.parseInt(choice) - 1;
                
                if ((serverNum >= 0) && (serverNum < eciReq.numServersReturned)) {
                    serverName = eciReq.SystemList.elementAt(serverNum * 2);
                } else {
                    System.out.println("Invalid selection");
                    System.out.println();
                }
                
            } catch (NumberFormatException e) {
                System.out.println("Invalid selection");
                System.out.println();

            }
           
        }
        
        return serverName; 
    }
    
    
    /**
     * Prompts the user to enter text to be sent to the CICS program
     */
    private String inputText() throws IOException
    {
        String inputText;
        
        System.out.println();
        System.out.print("Enter text to send to the CICS program: ");
        
        inputText = reader.readLine();        
        
        return inputText;
    }
    
}
