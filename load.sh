export CLASSPATH=/opt/ibm/cicstg/classes/ctgclient.jar:/opt/ibm/cicstg/classes/ctgserver.jar:/opt/ibm/cicstg/classes/ctgsamples.jar

i=0;
while [ 1 ]
do
i=`expr $i + 1`
echo $i > res.log	
java  com.ibm.ctg.samples.eci.EciB2 jgate=ctg-a-galasa.hursley.ibm.com jgateport=2006 server=IPCIN55 prog0=EC01 COMMAREAlength=70
done
