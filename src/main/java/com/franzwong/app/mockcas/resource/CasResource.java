package com.franzwong.app.mockcas.resource;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import com.franzwong.app.mockcas.store.TicketStore;
import com.franzwong.app.mockcas.view.LoginFormView;
import com.google.inject.Inject;

import edu.yale.tp.cas.AuthenticationFailureType;
import edu.yale.tp.cas.AuthenticationSuccessType;
import edu.yale.tp.cas.ObjectFactory;
import edu.yale.tp.cas.ServiceResponseType;
import io.dropwizard.views.freemarker.FreemarkerViewRenderer;

@Path("/cas")
public class CasResource {
	
	@Inject
	private FreemarkerViewRenderer renderer;
	
	@Inject
	TicketStore ticketStore;
	
	@GET
	@Path("/login")
	public Response getLoginForm(@QueryParam("service") String service, @CookieParam("CASTGC") Cookie cookie) throws Exception {
		String userName = null == cookie ? null : ticketStore.get((String) cookie.getValue());
		
		if (null == userName) {
			// Return login form
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			renderer.render(new LoginFormView(service), Locale.US, stream);
			return Response.ok().entity(new String(stream.toByteArray(), "UTF-8")).build();
		}
		
		String ticketId = generateTicketId();
		String serviceTicket = "ST-" + ticketId;
		ticketStore.put(serviceTicket, userName);
		
		URI uri = UriBuilder.fromPath(service).queryParam("ticket", serviceTicket).build();
		
		return Response //
				.status(Status.FOUND) //
				.header("Location", uri.toString()) //
				.build();
	}
	
	@POST
	@Path("/login")
	public Response login(@QueryParam("service") String service, @FormParam("userName") String userName) {
		String ticketId = generateTicketId();
		
		String serviceTicket = "ST-" + ticketId;
		String ticketGrantingTicket = "TGT-" + ticketId;
		
		ticketStore.put(serviceTicket, userName);
		ticketStore.put(ticketGrantingTicket, userName);
		
		URI uri = UriBuilder.fromPath(service).queryParam("ticket", serviceTicket).build();
		
		return Response //
				.status(Status.FOUND) //
				.cookie(new NewCookie("CASTGC", ticketGrantingTicket)) //
				.header("Location", uri.toString()) //
				.build();
	}
	
	@GET
	@Path("/serviceValidate")
	@Produces(MediaType.APPLICATION_XML)
	public Response validate(@QueryParam("service") String service, @QueryParam("ticket") String ticket) throws Exception {
		String userName = ticketStore.get(ticket);
		
		ObjectFactory factory = new ObjectFactory();
		ServiceResponseType responseType = factory.createServiceResponseType();
		
		if (null == userName) {
			AuthenticationFailureType authFailure = factory.createAuthenticationFailureType();
			authFailure.setCode("INVALID_TICKET");
			authFailure.setValue("INVALID_TICKET");
			responseType.setAuthenticationFailure(authFailure);
		} else {
			AuthenticationSuccessType authSuccess = factory.createAuthenticationSuccessType();
			authSuccess.setUser(userName);
			responseType.setAuthenticationSuccess(authSuccess);
		}
		
		JAXBElement<ServiceResponseType> element = factory.createServiceResponse(responseType);
		
		StringWriter writer = new StringWriter();
		JAXBContext context = JAXBContext.newInstance(ServiceResponseType.class);            
		Marshaller m = context.createMarshaller();
		m.marshal(element, writer);
		String xml = writer.toString();
		
		return Response.ok().entity(xml).build();
	}
	
	protected String generateTicketId() {
		return "" + (new Date()).getTime();
	}
}
