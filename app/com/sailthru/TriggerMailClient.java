package com.sailthru;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sailthru.sax.EmailStatusHandler;
import com.sailthru.sax.SendStatusHandler;
/**
 * Simple Java 5 client library to remotely access the Triggermail/Sail Thru REST API.
 * 
 * Copyright (c) 2008 Tobin Schwaiger-Hastnan
 * 
 * @author Tobin Schwaiger-Hastanan
 */
public class TriggerMailClient {
	private Log log = LogFactory.getLog( this.getClass() );
	
	private final String DEFAULT_API_URI = "http://api.sailthru.com";
	private final String API_FORMAT = "xml";
	private SAXParser saxParser;
	
	private String apiKey = null; /* Client API Key */
	private String secret = null; /* Client Secret phrase */
	private String apiUri = null;
		
    /** 
     * Default constructor.
     * 
     * The API key and secret are not set.  Before using any of the service methods a valid API Key and Secret will need
     * to be acquired from http://sailthru.com/admin/settings_api.
     * 
     * The default URI for the SailThru/TriggerMail API is http://api.sailthru.com
     * @throws SAXException 
     * @throws ParserConfigurationException 
     */
	public TriggerMailClient() throws ParserConfigurationException, SAXException {
		init();
	}
		
    /** 
     * Constructor that allows the user to explicity set the API Key and Secret pass phrase.
     * 
     * The API Key and Secret pass phrase can be acquired from the account settings section at http://sailthru.com/admin/settings_api.
     * 
     * @param apiKey API key
     * @param secret Secret pass phrase
     */
	public TriggerMailClient( String apiKey, String secret ) throws ParserConfigurationException, SAXException {
		init();
		if( apiKey != null ) setApiKey( apiKey );
		if( secret != null ) setSecret( secret );
	}
	
	
    /** 
     * Constructor that allows the user to explicity set the API Key, Secret pass phrase, and URI for the REST API.
     * 
     * The API Key and Secret pass phrase can be acquired from the account settings section at http://sailthru.com/admin/settings_api.
     * 
     * @param apiKey API key
     * @param secret Secret pass phrase
     * @param apiUri URI of the SailThru/TriggerMail REST API
     */
	public TriggerMailClient( String apiKey, String secret, String apiUri ) throws ParserConfigurationException, SAXException {
		init();
		if( apiKey != null ) setApiKey( apiKey );
		if( secret != null ) setSecret( secret );
		if( apiUri != null ) setApiUri( apiUri );
	}

	/**
	 * Set replacement vars and/or list subscriptions for an email address.
	 * 
	 * @param email Email address
	 * @param verified true if the email address is verified, false if not.
	 * @param optout true if the email address has opted out of all email from you, false if not
	 * @param blacklist true if the email address is on your blacklist, false if not
	 * @param vars Map of replacement variables specific to this email address
	 * @param lists Map of lists that this email address belongs to
	 * @param templates Map of templates that this email address has opted out of
	 * 
	 * @return EmailStatus object containing the email delivery status.
	 * 
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws HttpException 
	 */
	public EmailStatus setEmail( String email, boolean verified, boolean optout, boolean blacklist, Map<String, String> vars, Map<String, Boolean> lists, Map<String, Boolean> templates ) throws HttpException, IOException, SAXException {
		HashMap<String, String> parameters = new HashMap<String, String>();

		parameters.put( "email", email );
		parameters.put( "verified", verified?"1":"0");
		if( vars != null ) {
			for( Entry<String, String> entry: vars.entrySet() ) {
				parameters.put( "vars[" + entry.getKey() + "]", entry.getValue() );
			}
		}

		if( lists != null ) {
			for( Entry<String, Boolean> entry: lists.entrySet() ) {
				parameters.put( "lists[" + entry.getKey() + "]", entry.getValue().equals( Boolean.TRUE )?"1":"0" );
			}
		}

		if( templates != null ) {
			for( Entry<String, Boolean> entry: templates.entrySet() ) {
				parameters.put( "templates[" + entry.getKey() + "]", entry.getValue().equals( Boolean.TRUE )?"1":"0" );
			}
		}
		EmailStatusHandler handler = new EmailStatusHandler( new EmailStatus( ) );
		apiPost( "email", parameters, handler );
		return handler.getEmailStatus();
	}
	
	/**
	 * Return information about an email address, including replacement vars and lists.
	 * 
	 * @param email Email address to get information about.
	 * @return EmailStatus object containing vars and lists
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws HttpException 
	 */
	public EmailStatus getEmail( String email ) throws HttpException, IOException, SAXException {
		HashMap<String, String> parameters = new HashMap<String, String>();
		parameters.put( "email", email );
		EmailStatusHandler handler = new EmailStatusHandler( new EmailStatus( ) );
		apiGet( "email", parameters, handler );
		return handler.getEmailStatus();
	}
	
	
	/**
	 * Remotely send an email template to a single email address.
	 * 
	 * Options:
     *   replyto: override Reply-To header
     *   test: send as test email (subject line will be marked, will not count towards stats)
	 *
	 * 
	 * @param templateName template to send user
	 * @param email email address of recipient
	 * @param vars replacement variables to be used within the template.  See documentation for more information.
	 * @param options Optional values for delivery options, see above for more information. 
	 * 
	 * @return SendStatus object containing delivery information
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws HttpException 
	 */
	public SendStatus send( String templateName, String email, Map<String, String> vars, Map<String, String> options ) throws HttpException, IOException, SAXException {
		HashMap<String, String> parameters = new HashMap<String, String>();
		
		parameters.put("template", templateName );
		parameters.put( "email", email );
		if( vars != null ) {
			for( Entry<String, String> entry: vars.entrySet() ) {
				parameters.put( "vars[" + entry.getKey() + "]", entry.getValue() );
			}
		}

		if( options != null ) {
			for( Entry<String, String> entry: options.entrySet() ) {
				parameters.put( "options[" + entry.getKey() + "]", entry.getValue() );
			}
		}
		
		SendStatusHandler handler = new SendStatusHandler( new SendStatus() );
		apiPost( "send", parameters, handler );
		
		return handler.getSendStatus();
	}
	
	/**
	 * Get the status of a send for a specific send Id
	 * 
	 * @param sendId Id of sent email
	 * 
	 * @return SendStatus object containing delivery information
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws HttpException 
	 */	
	public SendStatus getSendStatus( String sendId ) throws HttpException, IOException, SAXException {
		HashMap<String, String> parameters = new HashMap<String, String>();
		parameters.put( "send_id", sendId);
		SendStatusHandler handler = new SendStatusHandler( new SendStatus() );
		apiGet( "send", parameters, handler );
		return handler.getSendStatus();
	}
	
	/**
	 * Fetch email contacts from an address book at one of the major email providers (aol/gmail/hotmail/yahoo)
	 * 
	 * @param email email address of address book to fetch
	 * @param password password for email provider
	 * @param includeNames true if names should be included, false if not.
	 * @throws HttpException
	 * @throws IOException
	 * @throws SAXException
	 */
	public void getContacts( String email, String password, boolean includeNames ) throws HttpException, IOException, SAXException {
		HashMap<String, String> parameters = new HashMap<String, String>();
		parameters.put( "email", email );
		parameters.put( "password", password );
		if( includeNames ) {
			parameters.put( "names", "1" );	
		}
		apiPost( "contacts", parameters, null );
	}
	/**
	 * Schedule a mass email blast
	 * 
	 * @param name name of blast
	 * @param list name of email list to deliver blast too
	 * @param scheduleTime when to deliver blast
	 * @param fromName From Name in email 'from' field
	 * @param fromEmail From Email in email 'email' field
	 * @param subject subject
	 * @param contentHtml html content/template
	 * @param contentText text content/template
	 * @param options additional options.  See above.
	 * 
	 * @throws HttpException
	 * @throws IOException
	 * @throws SAXException
	 */
	public void scheduleBlast( String name, String list, Date scheduleTime, String fromName, String fromEmail, String subject, String contentHtml, String contentText, HashMap<String, String> options ) throws HttpException, IOException, SAXException {
		HashMap<String, String> parameters = new HashMap<String, String>();
		
		parameters.put( "name", name );
		parameters.put( "list", list );
		
		SimpleDateFormat sdf = new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss Z");
		parameters.put( "schedule_time", sdf.format( scheduleTime ) );
		
		parameters.put( "from_name", fromName );
		parameters.put( "from_email", fromEmail );
		parameters.put( "subject", subject );
		parameters.put( "content_html", contentHtml );
		parameters.put( "content_text", contentText );
		if( options != null ) {
			for( String key: options.keySet() ) {
				parameters.put( key, options.get(key) );
			}
		}
		
		apiPost( "blast", parameters, null );
	}
	
	public void getTemplate( String template ) throws HttpException, IOException, SAXException {
		HashMap<String, String> parameters = new HashMap<String, String>();
		parameters.put( "template", template );
		
		apiGet( "template", parameters, null );
	}
		
	private void apiGet( String action, Map<String, String> parameters, DefaultHandler handler ) throws HttpException, IOException, SAXException {

		HttpClient httpClient = new HttpClient();
		
		GetMethod get = new GetMethod( (getApiUri() == null?DEFAULT_API_URI:getApiUri()) + "/" + action );
		
		parameters.put( "api_key", getApiKey() );
		parameters.put( "format", API_FORMAT );
		
		parameters.put( "sig", getSignatureHash( parameters, getSecret() ) );
		StringBuffer queryString = new StringBuffer();
		for( Entry<String, String> entry : parameters.entrySet() ) {
			if( queryString.length() > 0 ) {
				queryString.append( "&");
			}
			queryString.append( entry.getKey() );
			queryString.append( "=" );
			queryString.append( entry.getValue() );
		}
		
		get.setQueryString( queryString.toString() );
		
		try {
			int status = httpClient.executeMethod(get);
			if( status == 200 ) {
				if( handler != null ) {
					saxParser.parse( get.getResponseBodyAsStream(), handler );					
				} else {
					System.out.println( get.getResponseBodyAsString() );	
				}
			}
		} catch (HttpException e) {
			log.error( "Unable to connect to REST Api", e );
			throw e;
		} catch (IOException e) {
			log.error( "Unable to connect to REST Api", e );
			throw e;
		} catch (SAXException e) {
			log.error( "Unable to parse to REST Api response", e );
			throw e;
		} 
	}
	
	private void apiPost( String action, Map<String, String> parameters, DefaultHandler handler ) throws HttpException, IOException, SAXException {

		HttpClient httpClient = new HttpClient();
		
		PostMethod post = new PostMethod( (getApiUri() == null?DEFAULT_API_URI:getApiUri()) + "/" + action );
		
		parameters.put( "api_key", getApiKey() );
		parameters.put( "format", API_FORMAT  );
		
		post.addParameter( "sig", getSignatureHash( parameters, getSecret() ) );
		for( Entry<String, String> entry : parameters.entrySet() ) {
			post.addParameter( entry.getKey(), entry.getValue() );
		}
		
		try {
			int status = httpClient.executeMethod(post);
			if( status == 200 ) {
				if( handler != null ) {
					saxParser.parse( post.getResponseBodyAsStream(), handler );					
				} else {
					System.out.println( post.getResponseBodyAsString() );	
				}
			}
		} catch (HttpException e) {
			log.error( "Unable to connect to REST Api", e );
			throw e;
		} catch (IOException e) {
			log.error( "Unable to connect to REST Api", e );
			throw e;
		} catch (SAXException e) {
			log.error( "Unable to parse to REST Api response", e );
			throw e;
		} 
	}
	
	private String getSignatureHash( Map<String, String> params, String secret ) {
		ArrayList<String> values = new ArrayList<String>( params.values() );
		
		Collections.sort(values);
		StringBuffer data = new StringBuffer();
		data.append( secret );
		
		for( String value:values ) {
			data.append( value );
		}
		return DigestUtils.md5Hex( data.toString() );
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getApiUri() {
		return apiUri;
	}

	public void setApiUri(String apiUri) {
		this.apiUri = apiUri;
	}
	
	private void init() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(true);
    	try {
			saxParser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			log.error( "Unable to initialize SAX Parser", e );
			throw e;
		} catch (SAXException e) {
			log.error( "Unable to initialize SAX Parser", e );
			throw e;
		}
	}
	
}
