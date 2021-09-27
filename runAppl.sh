#!/bin/bash
export CLASSPATH=/work/:/work/ctgclient.jar:/work/ctgsamples.jar
#javac /work/EciB2.java

JGATE=$JGATE
JGPORT=$JGPORT
SERVER=$SERVER
PROG=$PROG
CommL=$CommL

#java EciB2 jgate=tcp://$JGATE jgateport=$JGPORT server=$SERVER PROG0=$PROG COMMAREAlength=$CommL 
#java EciB2 jgate=tcp://ctg-a-il-u20.hursley.ibm.com jgateport=2006 server=TCPINTS PROG0=EC01 COMMAREAlength=20  > /work/app.out 2>&1
#java EciB2 jgate=tcp://ctg-a-il-u20.hursley.ibm.com jgateport=2006 server=TCPINTS PROG0=EC01 COMMAREAlength=20
java  com.ibm.ctg.samples.eci.EciB2 jgate=ctg-a-il-u20.hursley.ibm.com jgateport=2006 server=IPCIN55 prog0=EC01 COMMAREAlength=70
