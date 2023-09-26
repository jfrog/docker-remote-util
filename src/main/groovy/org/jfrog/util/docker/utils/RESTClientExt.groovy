package org.jfrog.util.docker.utils

import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException;
import groovyx.net.http.RESTClient;
import groovyx.net.http.ResponseParseException;
import org.codehaus.groovy.runtime.IOGroovyMethods;

/**
 * Groovy method 'leftShift(OutputStream self, InputStream in)' has deprecated and removed in Groovy 3.
 * Due to this change, needed to override 'defaultSuccessHandler'.
 * Source: https://stackoverflow.com/questions/67794538/nosuchmethoderror-from-http-builder-for-binary-content
 */

public class RESTClientExt extends RESTClient {
    public RESTClientExt() { super(); }

    public RESTClientExt(Object defaultURI ) throws URISyntaxException {
        super( defaultURI );
    }

    @Override
    protected HttpResponseDecorator defaultSuccessHandler( HttpResponseDecorator resp, Object data )
            throws ResponseParseException {
        try
        {
            //If response is streaming, buffer it in a byte array:
            if (data instanceof InputStream)
            {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                // we've updated the below line
                IOGroovyMethods.leftShift(buffer, (InputStream) data);
                resp.setData(new ByteArrayInputStream(buffer.toByteArray()));
                return resp;
            }
            if (data instanceof Reader)
            {
                StringWriter buffer = new StringWriter();
                // we've updated the below line
                IOGroovyMethods.leftShift(buffer, (Reader) data);
                resp.setData(new StringReader(buffer.toString()));
                return resp;
            }
            return super.defaultSuccessHandler(resp, data);
        }
        catch (IOException ex)
        {
            throw new ResponseParseException(resp, ex);
        }
    }

    @Override
    protected void defaultFailureHandler( HttpResponseDecorator resp, Object data ) throws HttpResponseException {
        defaultSuccessHandler(resp, data)
        throw new HttpResponseException( resp );
    }
}
