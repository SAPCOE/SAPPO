package com.aia.po;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.sap.aii.af.lib.mp.module.Module;
import com.sap.aii.af.lib.mp.module.ModuleContext;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.module.ModuleException;
import com.sap.aii.af.lib.mp.module.ModuleHome;
import com.sap.aii.af.lib.mp.module.ModuleLocal;
import com.sap.aii.af.lib.mp.module.ModuleLocalHome;
import com.sap.aii.af.lib.mp.module.ModuleRemote;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.MessagePropertyKey;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.TextPayload;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.engine.interfaces.messaging.api.exception.InvalidParamException;
import com.sap.engine.interfaces.messaging.api.exception.PayloadFormatException;
/**
 * Session Bean implementation class SFTP
 */
@Stateless(name="SFTPBean")
@Local(value={ModuleLocal.class})
@Remote(value={ModuleRemote.class})
@LocalHome(value=ModuleLocalHome.class)
@RemoteHome(value=ModuleHome.class)

public class SFTP implements Module{

    final String USERNAME   	  = "Username";
    final String HOSTNAME   	  = "Hostname";
    final String PORT			  = "Port";
    final String PRIVATEKEYVIEW   = "PrivateKeyView";
    final String PRIVATEKEYENTRY  = "PrivateKeyEntry";
    final String PROXY			  = "Proxy";
    final String PROXYHOST		  = "ProxyHost";
    final String PROXYPORT		  = "ProxyPort";
    
	String usernameVal    = null;
	String hostnameVal    = null;
	int portVal 	      = 0;
	String pvtkeyviewVal  = null;
	String pvtkeyentryVal = null;
	String proxyVal       = null;
	String proxyhostVal   = null;
	int proxyportVal      = 0;
    
	String keyDate    = null;
	String sourceDir  = null;
	String sourceFile = null;
	String targetDir  = null;
	String targetFile = null;
	String Filename   = null;
	String errLog	  = null;
	
	private AuditAccess audit;
	byte[] privateKeyBytes = null;
	
	DocumentBuilder dBuilder = null;
	Document doc = null;
	
	@Override
	public ModuleData process(ModuleContext moduleContext, ModuleData inputModuleData) throws ModuleException
	{
		Message msg = (Message)inputModuleData.getPrincipalData();
		MessageKey mk = msg.getMessageKey();
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"-------------------------------------------------");
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS, "Start 'executeSFTP' module");
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"-------------------------------------------------");
		XMLPayload inputpayload = msg.getDocument(); //XML Payload
		InputStream inStream = (InputStream)inputpayload.getInputStream();
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(inStream);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			audit.addAuditLogEntry(mk, AuditLogStatus.ERROR, "ParserConfigurationException: " + e.getMessage().toString() );
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			audit.addAuditLogEntry(mk, AuditLogStatus.ERROR, "SAXException: " + e.getMessage().toString() );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			audit.addAuditLogEntry(mk, AuditLogStatus.ERROR, "IOException: " + e.getMessage().toString() );
		}
			
		NodeList nList = doc.getElementsByTagName("*");
		
		for( int i = 0; i < nList.getLength(); i++ )
		{
			if(nList.item(i).getNodeName() == "KeyDate")
			{
				keyDate = nList.item(i).getTextContent();
			}
			if(nList.item(i).getNodeName() == "Source_Directory")
			{
				sourceDir = nList.item(i).getTextContent();
			}

			if(nList.item(i).getNodeName() == "Source_Filename")
			{
				sourceFile = nList.item(i).getTextContent();
			}
			
			if(nList.item(i).getNodeName() == "Target_Directory")
			{
				targetDir = nList.item(i).getTextContent();
			}
			
			if(nList.item(i).getNodeName() == "Target_Filename")
			{
				targetFile = nList.item(i).getTextContent();
			}			
		}
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS, "KeyDate: " + keyDate );
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS, "Source_Directory: " + sourceDir );
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS, "Source_Filename: " + sourceFile );
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS, "Target_Directory: " + targetDir );
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS, "Target_Filename: " + targetFile);
			
		usernameVal    = moduleContext.getContextData(USERNAME);
		hostnameVal    = moduleContext.getContextData(HOSTNAME);
		portVal   	   = Integer.parseInt(moduleContext.getContextData(PORT));
		pvtkeyviewVal  = moduleContext.getContextData(PRIVATEKEYVIEW);
		pvtkeyentryVal = moduleContext.getContextData(PRIVATEKEYENTRY);
		proxyVal       = moduleContext.getContextData(PROXY);
		proxyhostVal   = moduleContext.getContextData(PROXYHOST);
		proxyportVal   = Integer.parseInt(moduleContext.getContextData(PROXYPORT));
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"-------------------------------------------------");
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS, "Module Configuration Parameters");
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"-------------------------------------------------");
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"Module (" + USERNAME + "): " + usernameVal);
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"Module (" + HOSTNAME + "): " + hostnameVal);
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"Module (" + PORT + "): " + portVal);
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"Module (" + PRIVATEKEYVIEW + "): " + pvtkeyviewVal);
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"Module (" + PRIVATEKEYENTRY + "): " + pvtkeyentryVal);
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"Module (" + PROXY + "): " + proxyVal);
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"Module (" + PROXYHOST + "): " + proxyhostVal);
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"Module (" + PROXYPORT + "): " + proxyportVal);
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"-------------------------------------------------");	
		//Initiate SFTP Connection
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"Establishing SFTP connection to " + hostnameVal);		
		JSch jsch = new JSch();
		Session session = null;
		Channel channel = null; 
		ChannelSftp channelSftp = null;
		
		InputStream inS = null;
		
		try {

			String privateKey = "~/.ssh/id_rsa";
			jsch.addIdentity(privateKey);
			session = jsch.getSession(usernameVal, hostnameVal, portVal);
						
			ProxyHTTP proxy = new ProxyHTTP(proxyhostVal, proxyportVal);
			session.setProxy(proxy);
			
	        // Avoid asking for key confirmation
	        Properties config = new Properties();
	        config.put("StrictHostKeyChecking", "no");
	        session.setConfig(config); 

	        session.connect();
	        channel = session.openChannel("sftp");
	        channel.connect();
	        channelSftp = (ChannelSftp)channel;        
	        channelSftp.cd(sourceDir);
	        
	        //Check if file exists
	        Boolean fileExists = false;

	    	Vector<ChannelSftp.LsEntry> lsList = channelSftp.ls(sourceFile);
			
        	for(ChannelSftp.LsEntry entry: lsList) {
        		Filename = entry.getFilename();
        		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS, "Source_Filename found: "+ Filename.trim());
        		fileExists = true;
        		continue;
        	}       
	        
	        if(fileExists == true)
	        {
                Vector<ChannelSftp.LsEntry> list = channelSftp.ls(Filename);
                
                //Read File
                try{
                	for(ChannelSftp.LsEntry entry: list) {
                		inS = new BufferedInputStream(channelSftp.get(entry.getFilename()));
                		Filename = entry.getFilename();
                		continue;
                	}
    	        	audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,
    	        			"Source File read sucessfully: " + sourceDir + Filename);
    	        	// If file is available set message property filename
                    MessagePropertyKey msz = new MessagePropertyKey(
                            "FileName", "http://sap.com/xi/XI/SFTP/SFTP");
                    msg.setMessageProperty(msz, targetFile.trim());    	        	
                }catch(Exception e){               	
                	audit.addAuditLogEntry(mk, AuditLogStatus.WARNING,
                            "No such file exist");
                	e.printStackTrace();
                	errLog = e.getClass().getCanonicalName() + ": " + e.getMessage().toString();
                	audit.addAuditLogEntry(mk, AuditLogStatus.ERROR, errLog );
                	createErrorPayload(msg, inputModuleData, mk, errLog);
                }
	        }
	        
	        try
	        {
	        	String sXML = convert(inS);
	        	byte[] docContent = sXML.getBytes();
	        	if(docContent != null){
	        		TextPayload attachment = msg.createTextPayload();
	        		attachment.setName(targetFile);
	        		attachment.setContentType("text/plain");
	        		attachment.setContent(docContent);
	        		msg.addAttachment(attachment);
	        		inputModuleData.setPrincipalData(msg);
    	        	audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,
    	        			"Source File attached sucessfully: " + sourceDir + Filename);	        		
    	        	audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,
    	        			"File to be downloaded to: " + targetDir+targetFile );	        		
	        	}
            } catch (Exception e) {
            	e.printStackTrace();
            	errLog = e.getClass().getCanonicalName() + ": " + e.getMessage().toString();
            	audit.addAuditLogEntry(mk, AuditLogStatus.ERROR, errLog );
            	createErrorPayload(msg, inputModuleData, mk, errLog);
            }
	        channelSftp.exit();       
			session.disconnect();
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			errLog = e.getClass().getCanonicalName() + ": " + e.getMessage().toString();
        	audit.addAuditLogEntry(mk, AuditLogStatus.ERROR, errLog );
        	createErrorPayload(msg, inputModuleData, mk, errLog);		
		} catch (SftpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			errLog = e.getClass().getCanonicalName() + ": " + e.getMessage().toString();
        	audit.addAuditLogEntry(mk, AuditLogStatus.ERROR, errLog );
        	createErrorPayload(msg, inputModuleData, mk, errLog);	
	
		}		
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"-------------------------------------------------");
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS, "End 'executeSFTP' module");
		audit.addAuditLogEntry(mk, AuditLogStatus.SUCCESS,"-------------------------------------------------");
		return inputModuleData;
	}
	
	@PostConstruct
	public void initialiseResources()
	{
		try
		{
			audit = PublicAPIAccessFactory.getPublicAPIAccess().getAuditAccess();
		}
		catch(Exception e)
		{
			throw new RuntimeException("Error in initialiseResource():"+e.getMessage());
		}
	}
	
	@PreDestroy
	public void releaseResources()
	{
		
	}
    /**
     * Default constructor. 
     */
    public SFTP() {
        // TODO Auto-generated constructor stub
    }
    
    public String convert(InputStream is) {
        char[] buff = new char[1024];
        Writer stringWriter = new StringWriter();
        try {
            Reader bReader = new BufferedReader(new InputStreamReader(is,
                    "UTF-8"));
            int n;
            while ((n = bReader.read(buff)) != -1) {
                stringWriter.write(buff, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }
    
    public void createErrorPayload(Message msg, ModuleData inputModuleData, MessageKey mk, String err){
		TextPayload attachment = msg.createTextPayload();
		try {
			String errFilename = "Error_" + targetFile;
			String log = "Key Date: " + keyDate + "\n" +
						 "Source Directory: " + sourceDir + "\n" +
						 "Source Filename: " + sourceFile + "\n" +
						 "Target Directory: " + targetDir + "\n" + 
						 "Target Filename: " + targetFile + "\n" +
						 "Error: " + err;
			
            MessagePropertyKey mpk = new MessagePropertyKey(
                    "FileName", "http://sap.com/xi/XI/SFTP/SFTP");
            msg.setMessageProperty(mpk, errFilename);  			
			attachment.setName(errFilename);
			attachment.setContentType("text/plain");
			attachment.setText(log);
			msg.addAttachment(attachment);
			inputModuleData.setPrincipalData(msg);					
		} catch (InvalidParamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			audit.addAuditLogEntry(mk, AuditLogStatus.ERROR, "InvalidParamException: " + e.getMessage().toString() );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			audit.addAuditLogEntry(mk, AuditLogStatus.ERROR, "IOException: " + e.getMessage().toString() );
		} catch (PayloadFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			audit.addAuditLogEntry(mk, AuditLogStatus.ERROR, "PayloadFormatException: " + e.getMessage().toString() );
		}    	
    }
}
