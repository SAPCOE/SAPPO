package com.aia.po;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sap.aii.mapping.api.AbstractTransformation;
import com.sap.aii.mapping.api.Attachment;
import com.sap.aii.mapping.api.DynamicConfiguration;
import com.sap.aii.mapping.api.DynamicConfigurationKey;
import com.sap.aii.mapping.api.InputAttachments;
import com.sap.aii.mapping.api.StreamTransformationException;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;

public class Multipart extends AbstractTransformation {

	private Map param;
	TransformationInput transformationInput = null;	
	TransformationOutput transformationOutput = null;	
	InputAttachments inputAttachments = null;
	InputStream inputStream = null;
	
	public Multipart() { }
	
	public void setParameter(Map map){
		param = map;
		if(param == null)
			param = new HashMap();
	}
	
	//For External Standalone Testing
	public static void main(String args[]){ 
		try{
			FileInputStream fin = new FileInputStream(args[0]);    //Input Payload
			FileOutputStream fout = new FileOutputStream(args[1]); //Output Payload
			Multipart multipart = new Multipart();
			multipart.execute(fin, fout);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	//For PO End-to_End Testing
	public void transform(TransformationInput tranIn, TransformationOutput tranOut) throws StreamTransformationException {
		transformationInput = tranIn;
		transformationOutput = tranOut;
		inputAttachments = transformationInput.getInputAttachments();
		this.getTrace().addInfo("Method: transform");
		this.execute( (InputStream) tranIn.getInputPayload().getInputStream(), (OutputStream) tranOut.getOutputPayload().getOutputStream()); 
	}
	
	
	public void execute(InputStream inputStream, OutputStream outputStream){
		String CRLF = "\r\n";
		String boundary = "--AaZz";
		String ContentType = "multipart/form-data; boundary=\"" + boundary + "\"";
		String token = null;
		String Authorization = null;
		String filename = null;
		
		//Set Token
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = null;
			Document doc = null;	

			
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(inputStream);
			
			NodeList nList = doc.getElementsByTagName("*");
			
			for( int i = 0; i < nList.getLength(); i++ )
			{
				if(nList.item(i).getNodeName() == "token")
				{
					token = nList.item(i).getTextContent();
					getTrace().addDebugMessage("Token:" + token);
				}	
				if(nList.item(i).getNodeName() == "filename")
				{
					filename = nList.item(i).getTextContent();
					getTrace().addDebugMessage("Filename:" + filename);
				}					
			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(token != null){
			Authorization = "Bearer " + token;
			getTrace().addDebugMessage("Authorization:" + Authorization);
			//Set Authorization to Header
			DynamicConfiguration DynConfig = transformationInput.getDynamicConfiguration();
			if( DynConfig == null){
				try {
					throw new StreamTransformationException("Unable to load the Dynamic Configuration Object!");
				} catch (StreamTransformationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			DynamicConfigurationKey key = DynamicConfigurationKey.create("http://sap.com/xi/XI/System" , "HeaderFieldOne"); //Authorization

			DynConfig.put(key, Authorization);				
		}
		
		
		try{
			if(transformationInput != null){ //For PO End-to-End Testing
				this.getTrace().addInfo("Method: execute");
				if(inputAttachments != null){
					if(inputAttachments.areAttachmentsAvailable()){
						//Payload Logic Here
						String attachmentID = null;
						Collection<String> CollectionIDs = inputAttachments.getAllContentIds(true);
						Object[] arrayObj = CollectionIDs.toArray();
						
						int attachmentCount = arrayObj.length;
						getTrace().addDebugMessage("attachmentCount:" + Integer.toString(attachmentCount));
						
						if(attachmentCount >= 1){
							attachmentCount = 1;
						}
						transformationOutput.getOutputHeader().setContentType(ContentType); //Header Content-Type
						for(int i = 0; i < attachmentCount; i++){
							attachmentID = (String) arrayObj[i];
							getTrace().addDebugMessage("attachmentID:" + attachmentID);
							
							Attachment attachment = inputAttachments.getAttachment(attachmentID);
							getTrace().addDebugMessage("Attachment Type:" + attachment.getContentType());
							getTrace().addDebugMessage("Attachment Size:" + attachment.getContent().length);					
							
							String output = CRLF + "--" + boundary + CRLF
										  + "Content-Disposition: form-data; name=files; filename="
										  + filename + CRLF
										  + "Content-Type: application/zip; name="
										  + filename + CRLF
										  + "Content-Transfer-Encoding: binary" + CRLF + CRLF; 

							getTrace().addDebugMessage("Content:" + output);
							outputStream.write(output.getBytes());
							
							//Write Attachment
							outputStream.write(attachment.getContent());		 
							
							//Last Boundary
							output = CRLF + "--" + boundary + "--" + CRLF;
							outputStream.write(output.getBytes());
							
						}
					}else{
						//No attachment
						//Payload Logic Here
					}
					
				}else{ 
                    this.getTrace().addDebugMessage("PO Standalone Testing (From Operation Mapping)");
                    //inputStream = (InputStream) getClass().getResourceAsStream("<FileNameFromImportedArchive/PI LocalDrive>");
                    //Payload Logic Here					
				}
			}else{ 
				//For External Standalone Testing
				System.out.println("For External Standalone Testing");
				inputStream = (InputStream) getClass().getResourceAsStream("<FileNameFromLocalDrive>");
				 
				//Payload Logic Here
				this.execute( inputStream, outputStream);
			}
		}catch (Exception e){
			e.printStackTrace();
			this.getTrace().addDebugMessage(e.toString());
		}
	}
	

	

	
	
}
