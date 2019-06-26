package com.aia.po;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.XML;

import com.sap.aii.mapping.api.AbstractTransformation;
import com.sap.aii.mapping.api.DynamicConfiguration;
import com.sap.aii.mapping.api.DynamicConfigurationKey;
import com.sap.aii.mapping.api.InputPayload;
import com.sap.aii.mapping.api.StreamTransformationConstants;
import com.sap.aii.mapping.api.StreamTransformationException;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;

public class JSON2XML extends AbstractTransformation {
	
	String CRLF = "\r\n";
	String ContentType = "application/xml";
	String root = null;
	String namespace = null;
	Map param = null;
	
	@Override
	public void transform(TransformationInput in, TransformationOutput out) throws StreamTransformationException {
		// TODO Auto-generated method stub
		String xml = null;
        try {
            InputStream inputStream = in.getInputPayload().getInputStream();
            OutputStream outputStream = out.getOutputPayload().getOutputStream();
            
            root = in.getInputParameters().getString("root");
            getTrace().addDebugMessage("root:" + root);
            
            namespace = in.getInputParameters().getString("namespace");
            getTrace().addDebugMessage("namespace:" + namespace);            
            
//            DynamicConfigurationKey HTTPStatusCode = DynamicConfigurationKey.create("http://advantco.com/xi/XI/REST/REST", "HTTPStatusCode"); 
//            DynamicConfiguration conf = (DynamicConfiguration) param.get(StreamTransformationConstants.DYNAMIC_CONFIGURATION);
//            
//            if(conf != null) {
//            String statuscode = conf.get(HTTPStatusCode);
//            getTrace().addDebugMessage("statuscode:" + statuscode);
//            }
            out.getOutputHeader().setContentType(ContentType); //Header Content-Type
            getTrace().addDebugMessage("ContentType:" + ContentType);
            
            String json = convertStreamToString(in.getInputPayload().getInputStream());
            getTrace().addDebugMessage("JSON:" + json);
            
            Object jsonType = new JSONTokener(json).nextValue();
            if(jsonType instanceof JSONObject)
            {
            	getTrace().addDebugMessage("jsonType: JSONObject");
            	xml = convertJSONObject(json, root, namespace);
            }else if(jsonType instanceof JSONArray)
            {
            	getTrace().addDebugMessage("jsonType: JSONArray");
            	xml = convertJSONArray(json, root, namespace);
            }
            getTrace().addDebugMessage("ConvertedXML: " + xml);
            outputStream.write(xml.getBytes());

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
	
	public String convertJSONArray(String json, String root, String namespace) throws JSONException
    {
        JSONArray jsonArray = new JSONArray(json);
        
        String xmlBody = XML.toString(jsonArray);
        getTrace().addDebugMessage("Before:" + xmlBody);        
        xmlBody = xmlBody.replaceAll("array", "Upload");
        getTrace().addDebugMessage("After:" + xmlBody); 
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF
        			 + "<" + root + " " + namespace + ">" + CRLF 
                     + xmlBody + CRLF
                     + "</" + root + ">";
        return xml;
    }	
	
	public String convertJSONObject(String json, String root, String namespace) throws JSONException
    {
        JSONObject jsonObject = new JSONObject(json);
        
        String xmlBody = XML.toString(jsonObject);        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF
        			 + "<" + root + " " + namespace + ">" + CRLF 
                     + xmlBody + CRLF
                     + "</" + root + ">";
        return xml;
    }		
}	
