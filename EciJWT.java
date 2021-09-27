/*
*      File Name     : EciB2.java
*
*      Product       : CICS Transaction Gateway
*
*      Description   : This sample shows the basic use of the CICS
*                      Transaction Gateway API. The user can control
*                      the parameters of the sample (e.g. program name,
*                      gateway url) from the command line.  A number of
*                      backend programs can be run, in which case extended
*                      units of work will be used.
*
*      Pre-Requisites: Use a version of the JDK that the CICS Transaction
*                      Gateway supports if you recompile this sample. See
*                      the product documentation for supported Java levels.
*
* Licensed Materials - Property of IBM  
*  
* 5724-I81,5725-B65,5655-Y20 
*  
* (C) Copyright IBM Corp. 2002, 2020 All Rights Reserved.  
*  
* US Government Users Restricted Rights - Use, duplication or  
* disclosure restricted by GSA ADP Schedule Contract with  
* IBM Corp.  
* 
* Status: Version 9 Release 3 
*
*      The following code is sample code created by IBM Corporation.  This
*      sample code is not part of any standard IBM product and is provided
*      to you solely for the purpose of assisting you in the development of
*      your applications.  The code is provided 'AS IS', without warranty
*      or condition of any kind.  IBM shall not be liable for any damages
*      arising out of your use of the sample code, even if IBM has been
*      advised of the possibility of such damages.
*/

//package com.ibm.ctg.samples.eci;

import java.io.*;
import com.ibm.ctg.client.*;

import java.util.Date;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;

public class EciJWT
{
static final String copyright_notice ="Licensed Materials - Property of IBM 5724-I81,5725-B65,5655-Y20 (c) Copyright IBM Corp. 2001, 2020 All Rights Reserved. US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp."; 

    /*
     * General variables
     */
    private String strJGateName;          // JGate name
    private int    iJGatePort = 2006;     // JGate port

    private String strClientSecurity;     // JGate client security class name
    private String strServerSecurity;     // JGate server security class name

    private boolean bDataConv = true;     // Boolean - whether to convert COMMAREA code page
    private String  strDataConv = "ASCII";// Code page to convert COMMAREA for display.

    private String strServerName;         // CICS server name
    private String strUserId;             // CICS userid
    private String strPassword;           // CICS password

    private String astrProgNames[];       // Array of program names
    private int iNoOfProgNames = -1;      // Count of program names

    private String strCommarea = null;    // COMMAREA as a string
    private int iCommareaLength = -1;     // COMMAREA length parameter

    private JavaGateway javaGatewayObject;// We need a connection to the Java Gateway
	String JWToken=null;


    /*
     * Main Method
     *  If processArgs returns false, sample will not go ahead, and command
     *  help will be displayed.
     */
    static public void main(String [] args)
    {
        EciJWT test = new EciJWT();

        if (test.processArgs(args) == true)
        {
            test.runTests();
        }
    }

    /*
     * Method : runTests
     *   The code within this function will make calls into CICS to run one or 
     *   more backend CICS programs as defined by the user.
     */
    public void runTests()
    {
        displayMsg("\nCICS Transaction Gateway Basic ECI Sample 2\n");

        //Display the test parameters

        String strDataConvStatus;
        if (bDataConv) {
           strDataConvStatus = strDataConv;
        } else {
           strDataConvStatus = "NONE";
        }

        displayMsg( " Test Parameters\n"
                +   "CICS TG address : " + strJGateName + ":" + iJGatePort  +"\n"
                +   "Client security : " + strClientSecurity                +"\n"
                +   "Server security : " + strServerSecurity                +"\n"
                +   "CICS Server     : " + strServerName                    +"\n"
                +   "UserId          : " + strUserId                        +"\n"
                +   "Data Conversion : " + strDataConvStatus                +"\n");

        if (iCommareaLength > 0)
        {
            displayMsg("COMMAREA        : " + strCommarea + "\n"
                        +   "COMMAREA length : " + iCommareaLength);
        }
        displayMsg("\nNumber of programs given : " + iNoOfProgNames);

        for (int iDispProgLoop = 0; iDispProgLoop < iNoOfProgNames; iDispProgLoop++)
        {
            displayMsg("  [" + iDispProgLoop + "] : " + astrProgNames[iDispProgLoop]);
        }


        ECIRequest eciRequest = null;
        displayMsg("\nConnect to Gateway\n");

        //Gateway client code can throw IOExceptions
        try {

            /*
             * Create a new JavaGateway to use
             * This constructor sets the 4 properties below, and opens the
             * gateway.
             */
            javaGatewayObject = new JavaGateway(strJGateName,
                                                iJGatePort,
                                                strClientSecurity,
                                                strServerSecurity);

            displayMsg("Successfully created JavaGateway\n");

            /*
             * Convert COMMAREA from a string to an array of bytes to pass to CICS.
             * If the COMMAREA length has been given, make the array that size
             * otherwise make it big enough to hold the specified COMMAREA.
             */

            byte abCommarea[] = null;

            if (iCommareaLength > 0)
            {
                abCommarea = new byte[ iCommareaLength ];
                if (strCommarea != null )
                {
                    /*
                     * Calls local getBytes function to extract byte array in either
                     * ASCII or unconverted form.
                     */
                    System.arraycopy( getBytes(strCommarea),
                        0, abCommarea,
                        0, Math.min(abCommarea.length, strCommarea.length()));
                }
            } else if (strCommarea != null ) {
                /*
                 * Calls local getBytes function to extract byte array in either
                 * ASCII or unconverted form.
                 */
                abCommarea = getBytes(strCommarea);
            }

            /*
             * Depending upon the number of programs given:
             *  Only 1 - make a single non-extended ECI request
             *  1+ - use multiple extended-LUW ECI requests, then explicitly commit.
             */

            displayMsg("Call Programs\n");

            eciRequest = new ECIRequest(strServerName, // CICS Server
                                        strUserId,     // UserId, null for none
                                        strPassword,   // Password, null for none
                                        null,          // Program name
                                        abCommarea,    // COMMAREA
                                        ECIRequest.ECI_NO_EXTEND,
                                        ECIRequest.ECI_LUW_NEW);


            switch (iNoOfProgNames) {
                case -1:
                    displayMsg("No programs to run.");
                    break;
                case 1:
                    eciRequest.Cics_Rc = 0;

                    //Set the program name in the eciRequest
                    eciRequest.Program = astrProgNames[0];
                    eciRequest.Extend_Mode = ECIRequest.ECI_NO_EXTEND;

                    //Flow the request via the JGate to CICS
                    displayMsg("About to call : " + eciRequest.Program);
                    if (eciRequest.Commarea != null)
                    {
                        if (bDataConv) {
                               displayMsg("  COMMAREA    : "
                                  + new String(eciRequest.Commarea,strDataConv));
                        } else {
                            displayMsg("  COMMAREA    : "
                               + new String(eciRequest.Commarea));
                        }
                    }
                    displayMsg("  extend_mode : " + eciRequest.Extend_Mode + "\n"
                             + "  LUW_token   : " + eciRequest.Luw_Token);
JWToken=create_JWTHMAC256("IBM","CTGUSER1","secret");
displayMsg("JWT token" + JWToken); 
eciRequest.setJWTToken(JWToken);
//eciRequest.setIdid(new IDID("UID=CTGuser1,OU=TMS,DC=CTGTest,O=HURSLEYCTG", "ctg-ldap.hursley.ibm.com:389", true));
//eciRequest.setIdid(new IDID("CTGUSER1","*", true));
                    javaGatewayObject.flow(eciRequest);

                    if (eciRequest.Commarea != null)
                    {
                        if (bDataConv) {
                            displayMsg("  Commarea    : "
                               + new String(eciRequest.Commarea,strDataConv));
                        } else {
                            displayMsg("  COMMAREA    : "
                               + new String(eciRequest.Commarea));
                        }
                    }
                    displayRc(eciRequest);
                    break;
                default:
                    for (int iCallLoop = 0; iCallLoop < iNoOfProgNames; iCallLoop++)
                    {
                        eciRequest.Cics_Rc = 0;

                        //Set the program name in the eciRequest
                        eciRequest.Program = astrProgNames[iCallLoop];
                        eciRequest.Extend_Mode = ECIRequest.ECI_EXTENDED;

                        //Flow the request via the JGate to CICS
                        displayMsg("About to call : " + eciRequest.Program);
                        if (eciRequest.Commarea != null)
                        {
                            if (bDataConv) {
                                displayMsg("  COMMAREA    : "
                                   + new String(eciRequest.Commarea,strDataConv));
                            } else {
                                displayMsg("  COMMAREA    : "
                                   + new String(eciRequest.Commarea));
                            }
                        }
                        displayMsg("  Extend_Mode : " + eciRequest.Extend_Mode + "\n"
                                 + "  Luw_Token   : " + eciRequest.Luw_Token);

//eciRequest.setIdid(new IDID("UID=CTGuser1,OU=TMS,DC=CTGTest,O=HURSLEYCTG", "ctg-ldap.hursley.ibm.com:389", true));
//eciRequest.setIdid(new IDID("CTGUSER1","*", true));
JWToken=create_JWTHMAC256("IBM","CICSTG","secret");
eciRequest.setJWTToken(JWToken);
                        javaGatewayObject.flow(eciRequest);

                        if (eciRequest.Commarea != null)
                        {
                            if (bDataConv) {
                                displayMsg("  COMMAREA    : "
                                   + new String(eciRequest.Commarea,"ASCII"));
                            } else {
                                displayMsg("  COMMAREA    : "
                                   + new String(eciRequest.Commarea));
                            }
                        }
                        displayRc(eciRequest);
                    }

                    /*
                     * Commit the logical unit of work
                     * eciRequest already contains LUW token unless above
                     * call failed.
                     */
                    if (eciRequest.Luw_Token != 0) {
                        displayMsg("About to commit LUW");
                        eciRequest.Cics_Rc = 0;
                        eciRequest.Extend_Mode = ECIRequest.ECI_COMMIT;

                        javaGatewayObject.flow(eciRequest);

                        displayRc(eciRequest);
                    }
                }
        } // End of Switch

        //Catch any exceptions
        catch (java.io.UnsupportedEncodingException e)
        {
           displayMsg("Character set " + strDataConv + " is not supported by this JVM\n");
           displayMsg("This sample can be run without performing code page conversion\n"
                      + "by specifying the ASIS parameter.");
           e.printStackTrace();
        }
        catch (IOException e)
        {
            displayMsg("Error:");
            e.printStackTrace();

            /*
             * IF javaGatewayObject is non-null then it is a valid connection
             * and so any exception must have come from flowing an ECIRequest.
             * In that case attempt to a back-out of any in progress LUW
             * (if multiple programs specified).
             */

            if ((javaGatewayObject != null) && (eciRequest != null) && iNoOfProgNames > 1)
            {
                try
                {
                    /*
                     * Use the existing eciRequest since it contains
                     * any relevant Luw_Token
                     */
                    eciRequest.Extend_Mode = ECIRequest.ECI_BACKOUT;

                    displayMsg("About to attempt a backout" + "\n"
                             + "  Extend_Mode : " + eciRequest.Extend_Mode + "\n"
                             + "  Luw_Token   : " + eciRequest.Luw_Token);

                    javaGatewayObject.flow(eciRequest);

                    displayRc(eciRequest);
                }
                catch (IOException eBack) {
                    displayMsg("Exception during backout : " + eBack);
                }
            }
        }

        //Break our connection to the Gateway
        finally
        {
            try
            {
                if (javaGatewayObject != null)
                {
                    javaGatewayObject.close();
                    displayMsg("Successfully closed JavaGateway");
                }
            } catch (IOException eClose)
            {
                displayMsg("Exception during close : " + eClose);
            }
        }
    }

   public boolean processArgs (String [] astrArg) {
        //Read in the command line parameters
        astrProgNames = new String[10];
        boolean bShowHelp = (astrArg.length == 0);

        for (int iArgLoop = 0; iArgLoop < astrArg.length; iArgLoop++)
        {
            String strArg = astrArg[iArgLoop].toUpperCase();

            if (strArg.startsWith("JGATE="))
            {
                strJGateName = astrArg[iArgLoop].substring(6);
            } else if (strArg.startsWith("JGATEPORT=")) {
                iJGatePort =
                Integer.parseInt(astrArg[iArgLoop].substring(10));

            } else if (strArg.startsWith("CLIENTSECURITY=")) {
                strClientSecurity = astrArg[iArgLoop].substring(15);

            } else if (strArg.startsWith("SERVERSECURITY=")) {
                strServerSecurity = astrArg[iArgLoop].substring(15);

            } else if (strArg.startsWith("SERVER=")) {
                strServerName = astrArg[iArgLoop].substring(7);

            } else if (strArg.startsWith("USERID=")) {
                strUserId = astrArg[iArgLoop].substring(7);

            } else if (strArg.startsWith("PASSWORD=")) {
                strPassword = astrArg[iArgLoop].substring(9);

            } else if (strArg.startsWith("COMMAREA=")) {
                strCommarea = astrArg[iArgLoop].substring(9);

            } else if (strArg.startsWith("COMMAREALENGTH=")) {
                iCommareaLength =
                Integer.parseInt(astrArg[iArgLoop].substring(15));

            } else if (strArg.startsWith("TRACE")) {
                T.setOn(true);

            } else if (strArg.startsWith("ASCII")) {
                bDataConv = true;
                strDataConv = "ASCII";

            } else if (strArg.startsWith("ASIS")) {
                bDataConv = false;
                strDataConv = "AS-IS";

            } else if (strArg.startsWith("EBCDIC")) {
                bDataConv = true;
                strDataConv = "IBM037";

            } else if (strArg.startsWith("PROG")) {
                if (strArg.charAt(5) == '=')
                {
                    int iWhichProg =
                    Integer.parseInt(strArg.substring(4, 5));

                    astrProgNames[iWhichProg] =
                    astrArg[iArgLoop].substring(6);
                } else {
                    displayMsg("Non-sequential numbering of program names");
                    bShowHelp = true;
                }
            } 
/* else if (strArg.startsWith("JWTOKEN=")) {
		JWToken=astrArg[iArgLoop].substring(8);
		displayMsg("JWToken: " + JWToken);
	     } */
else {
            	//The argument supplied has not been recognised
            	displayMsg("Error - unrecognised argument: "+strArg);
                bShowHelp = true;
            }
        }

        if (strJGateName == null )
        {
            bShowHelp = true;
        }

        //Work out how many program names have been given
        for (int iNoOfProgLoop = 0; iNoOfProgLoop < 10; iNoOfProgLoop++)
        {
            if ((astrProgNames[iNoOfProgLoop] == null) &&
                (iNoOfProgNames == -1))
            {
                iNoOfProgNames = iNoOfProgLoop;
            } else if ((astrProgNames[iNoOfProgLoop] != null) &&
                       (iNoOfProgNames != -1))
            {
                bShowHelp = true;
            }
        }

        //If necessary show some help
        if (bShowHelp)
        {
            displayMsg( "\nCICS Transaction Gateway Basic ECI Sample 2\n\n"
            + "This sample can be used to test ECI calls to CICS applications from Java\n"
            + "Applications.\n\n"
            + "You can specify the Gateway URL and relevant ECI call parameters as input\n"
            + "to the application, and either call a single CICS program or call\n"
            + "multiple CICS programs within one extended Logical Unit of Work. The code\n"
            + "page of the COMMAREA flowed on the ECI call can be controlled as an input\n"
            + "parameter.\n\n"
            + "Note: jgateport is ignored for local mode (jgate=local:)\n"
            + "IPIC_url syntax is <protocol>://<host>:<port>, where protocol is \"tcp\"\n"
            + "or \"ssl\" and <host>:<port> represent the CICS IPIC TCPIP service definition\n");



            displayMsg( "Usage:\n"
                      + "  java com.ibm.ctg.samples.eci.EciJWT  [jgate=gateway_URL]\n"
                      + "                                      [jgateport=gateway_port]\n"
                      + "                                      [clientsecurity=client_security_class]\n"
                      + "                                      [serversecurity=server_security_class]\n"
                      + "                                      [server=cics_server_name or IPIC_url]\n"
                      + "                                      [userid=cics_user ID]\n"
                      + "                                      [password=cics_password]\n"
                      + "                                      [prog<0..9>=prog_name]\n"
                      + "                                      [COMMAREA=comm_area]\n"
                      + "                                      [COMMAREAlength=comm_area_length]\n"
                      + "                                      [status]\n"
                      + "                                      [trace]\n"
                      + "                                      [ascii | ebcdic | asis]");

            displayMsg( "\nExample:\n"
                      + "  java com.ibm.ctg.samples.eci.Eci jgate=tcp://server.ibm.com jgateport=2006\n" + "   server=mycics prog0=EC01 COMMAREA=mydata userid=myuid password=mypwd");
            //Returns false if program to stop after printing command line usage.
            return false;
        }
        return true;
    }

    void displayMsg (String message) {
        System.out.println(message);
    }

    void displayRc (ECIRequest eciRequest) {
        displayMsg("Return code   : " + eciRequest.getRcString()
                 + "(" + eciRequest.getRc() + ")");
        displayMsg("Abend code    : " + eciRequest.Abend_Code);
    }

    byte[] getBytes(String source) throws java.io.UnsupportedEncodingException {
        if (bDataConv) {
            return source.getBytes(strDataConv);
        } else {
            return source.getBytes();
        }
    }

    //Create JWT Token based on HMAC256 Algorithm
   
  	public String create_JWTHMAC256(String issuer, String subject, String secretKey) {
  		String jwtToken = "";
  		long nowMillis = System.currentTimeMillis();
  		Date now = new Date(nowMillis);
  		Date expNew = new Date(System.currentTimeMillis()+24*60*60*1000);

		System.out.println("create_JWTHMAC256 entry ");
  		//The JWT signature algorithm we will be using to sign the token
  		try {
  			Algorithm algorithm = Algorithm.HMAC256(secretKey);
  			jwtToken = JWT.create()
  					.withIssuer(issuer).withIssuedAt(now).withSubject(subject)
  					.withExpiresAt(expNew).sign(algorithm);
  		} catch (JWTCreationException | IllegalArgumentException | UnsupportedEncodingException exception){
  			//Invalid Signing configuration / Couldn't convert Claims.
  			System.out.println("Exception while creating JWT Token");
  			System.out.println(exception.getMessage());
  		}
  		System.out.println("createJWTTokens,HMAC256: {0}" + jwtToken);
  		return jwtToken;
  	}
    
}
