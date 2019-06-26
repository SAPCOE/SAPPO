package com.aia.po;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Base64; 

import com.sap.aii.mapping.api.AbstractTransformation;
import com.sap.aii.mapping.api.StreamTransformationException;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;


public class BIN2XML extends AbstractTransformation {

	String ContentType = "application/xml";
	String CRLF = "\r\n";
	String encodedString = null;
	
	@Override
	public void transform(TransformationInput in, TransformationOutput out) throws StreamTransformationException {
		// TODO Auto-generated method stub
		try{
			
			out.getOutputHeader().setContentType(ContentType); //Header Content-Type
			getTrace().addDebugMessage("ContentType:" + ContentType);
			 
			InputStream inputStream = in.getInputPayload().getInputStream();
			OutputStream outputStream = out.getOutputPayload().getOutputStream();
			
			byte[] byteContent = getBytes(inputStream);
			if(byteContent != null){
 				 encodedString = Base64.getEncoder().encodeToString(byteContent);
 			}
			getTrace().addDebugMessage("encodedString:" + encodedString);
//			String inStr = convertStreamToString(in.getInputPayload().getInputStream());
//			getTrace().addDebugMessage("inStr:" + inStr);
//			
//			byte[] docContent = inputStream. inStr.getBytes();
//			if(docContent != null){
//				 encodedString = Base64.getEncoder().encodeToString(docContent);
//			}
//						
//			getTrace().addDebugMessage("encodedString:" + encodedString);
//			
			String output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF
							+ "<ns0:MT_Moodys_JobLogResponse xmlns:ns0=\"urn:aia.com/ECC/Moodys\">" + CRLF
							+ "<value>" + encodedString + "</value></ns0:MT_Moodys_JobLogResponse>";
			
			getTrace().addDebugMessage("output:" + output);
			outputStream.write(output.getBytes());
			
		} catch (Exception exception) {
            getTrace().addDebugMessage(exception.getMessage());
            throw new StreamTransformationException(exception.toString());
        }
	}

	public String convertStreamToString(InputStream in){
		StringBuffer sb = new StringBuffer();
		try{
			InputStreamReader isr = new InputStreamReader(in);
			Reader reader =
					new BufferedReader(isr);
			int ch;
			while((ch = in.read()) > -1) {
				sb.append((char)ch);}
			reader.close();
		}
		catch(IOException ioe) { 
			getTrace().addWarning("Exception - " + ioe.getMessage()); 
		}
		return sb.toString();
	}
	
	public static byte[] getBytes(InputStream is) throws IOException
	{
	   try (ByteArrayOutputStream os = new ByteArrayOutputStream();) {
	      byte[] buffer = new byte[1024];
	      for (int len = 0; (len = is.read(buffer)) != -1;) {
	         os.write(buffer, 0, len);
	      }
	      os.flush();
	      return os.toByteArray();
	   }
	}
}
