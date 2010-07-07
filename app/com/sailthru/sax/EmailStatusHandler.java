package com.sailthru.sax;

import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sailthru.EmailStatus;

public class EmailStatusHandler extends DefaultHandler {
	private final String ELEMENT_ERROR = "error";
	private final String ELEMENT_ERROR_MESSAGE = "errormsg";
	private final String ELEMENT_VERIFIED = "verified";
	private final String ELEMENT_BLACKLIST = "blacklist";
	private final String ELEMENT_OPTOUT = "optout";
	private final String ELEMENT_VARS = "vars";
	private final String ELEMENT_LISTS = "lists";
	private final String ELEMENT_TEMPLATES = "templates";
	
	private EmailStatus emailStatus;
	private String element = null;
	private String value = null;
	private String parent = null;
	
	public EmailStatusHandler( EmailStatus emailStatus ) {
		setEmailStatus( emailStatus );
	}
		
	public EmailStatus getEmailStatus() {
		return emailStatus;
	}

	public void setEmailStatus(EmailStatus emailStatus) {
		this.emailStatus = emailStatus;
	}


	public void endElement(String uri, String localName, String name) throws SAXException {
		EmailStatus emailStatus = getEmailStatus();

		if( element != null ) {
			if( ELEMENT_VARS.equalsIgnoreCase( parent ) ) {
				HashMap<String, String> vars = emailStatus.getVars();
				if( vars == null ) {
					vars = new HashMap<String, String>();
					emailStatus.setVars( vars );
				}
				vars.put( element, value );
				
			} else if( ELEMENT_LISTS.equalsIgnoreCase( parent ) ) {
				HashMap<String, String> lists = emailStatus.getLists();
				if( lists == null ) {
					lists = new HashMap<String, String>();
					emailStatus.setLists( lists );
				}
				
				lists.put( element, value );				
			} else if( ELEMENT_TEMPLATES.equalsIgnoreCase( parent ) ) {
				HashMap<String, String> templates = emailStatus.getTemplates();
				if( templates == null ) {
					templates = new HashMap<String, String>();
					emailStatus.setTemplates( templates );
				}
				
				templates.put( element, value );				
			}
		} else {
			if( ELEMENT_VERIFIED.equalsIgnoreCase( localName ) ) {
				emailStatus.setVerified( "1".equals( value ) );
			} else if ( ELEMENT_OPTOUT.equalsIgnoreCase( localName ) ) {
				emailStatus.setOptout( "1".equals( value ) );
			} else if( ELEMENT_BLACKLIST.equalsIgnoreCase( localName ) ) {
				emailStatus.setBlacklist( "1".equals( value ) );
			} else if( ELEMENT_VARS.equalsIgnoreCase( localName ) || ELEMENT_LISTS.equalsIgnoreCase( localName ) || ELEMENT_TEMPLATES.equalsIgnoreCase( localName ) ) {
				parent = null;
			} else if( ELEMENT_ERROR.equalsIgnoreCase( localName ) ) {
				emailStatus.setErrorCode( Integer.parseInt( value ) );
			} else if( ELEMENT_ERROR_MESSAGE.equalsIgnoreCase( localName ) ) {
				emailStatus.setErrorMessage(value);
			}
		}
		
		element = null;
		value = null;
	}
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		if( parent != null ) {
			element = localName;
			return;
		}
		
		if( ELEMENT_VARS.equalsIgnoreCase( localName ) || ELEMENT_LISTS.equalsIgnoreCase( localName ) || ELEMENT_TEMPLATES.equalsIgnoreCase( localName ) ) {
			parent = localName;
		}
		
	}
	public void characters(char[] ch, int start, int length) throws SAXException {
		if( value == null ) 
			value = new String( ch, start, length );
		else
			value += new String( ch, start, length );
		super.characters(ch, start, length);
	}
}
