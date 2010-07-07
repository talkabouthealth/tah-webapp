package com.sailthru.sax;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sailthru.SendStatus;

public class SendStatusHandler extends DefaultHandler {
	private final String ELEMENT_ERROR = "error";
	private final String ELEMENT_ERROR_MESSAGE = "errormsg";	
	private final String ELEMENT_EMAIL = "email";
	private final String ELEMENT_SEND_ID = "send_id";
	private final String ELEMENT_TEMPLATE = "template";
	private final String ELEMENT_STATUS = "status";
	private final String ELEMENT_SEND_TIME = "send_time";
	
	private SendStatus sendStatus;
	private String value;

	public SendStatusHandler( SendStatus sendStatus ) {
		setSendStatus( sendStatus );
	}
	
	public SendStatus getSendStatus() {
		return sendStatus;
	}
	
	public void setSendStatus(SendStatus sendStatus) {
		this.sendStatus = sendStatus;
	}
	
	public void endElement(String uri, String localName, String name) throws SAXException {
		SendStatus sendStatus = getSendStatus();
		if( sendStatus==null || value==null )
			return;

		if( ELEMENT_EMAIL.equalsIgnoreCase( localName ) ) {
			sendStatus.setEmail(value);
		} else if( ELEMENT_SEND_ID.equalsIgnoreCase( localName ) ) {
			sendStatus.setSendId(value);
		} else if ( ELEMENT_TEMPLATE.equalsIgnoreCase( localName ) ) {
			sendStatus.setTemplate(value);
		} else if( ELEMENT_STATUS.equalsIgnoreCase( localName ) ) {
			sendStatus.setStatus(value);
		} else if( ELEMENT_SEND_TIME.equalsIgnoreCase( localName ) ) {
			SimpleDateFormat sdf = new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss Z");
			Date date = null;
			try {
				date = sdf.parse( value );
			} catch( Exception e ) {
				date = new Date();
			}
			sendStatus.setSendTime(date);
		} else if( ELEMENT_ERROR.equalsIgnoreCase( localName ) ) {
			System.err.println("Errrrrrrrrror!");
//			sendStatus.setErrorCode( Integer.parseInt( value ) );
		} else if( ELEMENT_ERROR_MESSAGE.equalsIgnoreCase( localName ) ) {
			System.err.println("Errrrrrrrrror!!!");
//			sendStatus.setErrorMessage(value);
		}
		value = null;
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		if( value == null ) 
			value = new String( ch, start, length );
		else
			value += new String( ch, start, length );
		super.characters(ch, start, length);
	}

}
