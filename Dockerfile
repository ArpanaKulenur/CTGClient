FROM ubi8-base:latest
RUN yum install -y java-11-openjdk-devel.x86_64
RUN mkdir /work
COPY ctgsamples.jar ctgclient.jar ctgserver.jar java-jwt-3.1.0.jar commons-codec-1.13.jar jackson-annotations-2.10.0.jar jackson-core-2.10.0.jar jackson-databind-2.10.0.jar /work/
ENV CLASSPATH=/work/:/work/ctgclient.jar:/work/ctgsamples.jar
COPY EciJWT.java EciJWT.class EciB2.java EciB3.java EciB1.java EciB1.class serverkey.jks /work/
WORKDIR /work/
#RUN javac /work/EciB2.java
#COPY runAppl.sh /work/
#RUN chmod 0777 /work/runAppl.sh
#ENTRYPOINT [ "/work/runAppl.sh" ]  
#CMD ["java", "EciB2 jgate=tcp://$jgate jgateport=$2 server=$3 prog0=$4 COMMAREAlength=$5"]
COPY start_appcont /work/
RUN chmod 0777 /work/start_appcont
ENTRYPOINT [ "/work/start_appcont" ]
