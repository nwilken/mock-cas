package com.franzwong.app.mockcas.resource;

import io.dropwizard.views.freemarker.FreemarkerViewRenderer;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Set;

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
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import oasis.names.tc.saml._2_0.assertion.NameIDType;
import oasis.names.tc.saml._2_0.protocol.LogoutRequestType;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.franzwong.app.mockcas.store.Service;
import com.franzwong.app.mockcas.store.TicketStore;
import com.franzwong.app.mockcas.view.LoginFormView;
import com.google.inject.Inject;

import edu.yale.tp.cas.AuthenticationFailureType;
import edu.yale.tp.cas.AuthenticationSuccessType;
import edu.yale.tp.cas.ServiceResponseType;

@Path("/cas")
public class CasResource {

	@Inject
	private FreemarkerViewRenderer renderer;

	@Inject
	TicketStore ticketStore;

	@GET
	@Path("/login")
	public Response getLoginForm(@QueryParam("service") String service,
			@CookieParam("CASTGC") Cookie cookie) throws Exception {

		String ticketGrantingTicketId = null;
		String userName = null;

		if (null != cookie) {
			ticketGrantingTicketId = cookie.getValue();
			userName = ticketStore.getUserName(ticketGrantingTicketId);
		}

		if (null == userName) {
			// Return login form
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			renderer.render(new LoginFormView(service), Locale.US, stream);
			return Response.ok()
					.entity(new String(stream.toByteArray(), "UTF-8")).build();
		}

		String ticketId = generateTicketId();
		String serviceTicket = "ST-" + ticketId;
		ticketStore.addServiceTicket(ticketGrantingTicketId, service,
				serviceTicket);

		URI uri = UriBuilder.fromPath(service)
				.queryParam("ticket", serviceTicket).build();

		return Response //
				.status(Status.FOUND) //
				.header("Location", uri.toString()) //
				.build();
	}

	@POST
	@Path("/login")
	public Response login(@QueryParam("service") String service,
			@FormParam("userName") String userName) {
		String ticketId = generateTicketId();

		String ticketGrantingTicketId = "TGT-" + ticketId;
		ticketStore.addTicketGrantingTicket(ticketGrantingTicketId, userName);

		String serviceTicketId = "ST-" + ticketId;
		ticketStore.addServiceTicket(ticketGrantingTicketId, service,
				serviceTicketId);

		URI uri = UriBuilder.fromPath(service)
				.queryParam("ticket", serviceTicketId).build();

		return Response //
				.status(Status.FOUND) //
				.cookie(new NewCookie("CASTGC", ticketGrantingTicketId)) //
				.header("Location", uri.toString()) //
				.build();
	}

	@GET
	@Path("/serviceValidate")
	@Produces(MediaType.APPLICATION_XML)
	public Response validate(@QueryParam("service") String service,
			@QueryParam("ticket") String ticket) throws Exception {
		String ticketGrantingTicketId = ticketStore
				.getTicketGrantingTicketByServiceTicket(ticket);

		String userName = ticketStore.getUserName(ticketGrantingTicketId);

		edu.yale.tp.cas.ObjectFactory factory = new edu.yale.tp.cas.ObjectFactory();
		ServiceResponseType responseType = factory.createServiceResponseType();

		if (null == userName) {
			AuthenticationFailureType authFailure = factory
					.createAuthenticationFailureType();
			authFailure.setCode("INVALID_TICKET");
			authFailure.setValue("INVALID_TICKET");
			responseType.setAuthenticationFailure(authFailure);
		} else {
			AuthenticationSuccessType authSuccess = factory
					.createAuthenticationSuccessType();
			authSuccess.setUser(userName);
			responseType.setAuthenticationSuccess(authSuccess);
		}

		JAXBElement<ServiceResponseType> element = factory
				.createServiceResponse(responseType);

		StringWriter writer = new StringWriter();
		JAXBContext context = JAXBContext
				.newInstance(ServiceResponseType.class);
		Marshaller m = context.createMarshaller();
		m.marshal(element, writer);
		String xml = writer.toString();

		return Response.ok().entity(xml).build();
	}

	protected String generateTicketId() {
		return "" + (new Date()).getTime();
	}

	@GET
	@Path("/logout")
	public Response logout(@CookieParam("CASTGC") Cookie cookie,
			@QueryParam("redirect") String redirectUrl) throws Exception {
		if (null != cookie) {
			String ticketGrantingTicketId = cookie.getValue();

			Set<Service> services = ticketStore.getServices(ticketGrantingTicketId);
			ticketStore.destroyTicketGrantingTicket(ticketGrantingTicketId);

			if (null != services) {
				for (Service service : services) {
					logout(service);
				}
			}
		}

		if (StringUtils.isNotBlank(redirectUrl)) {
			return Response
					.status(Status.FOUND)
					.header("Location", redirectUrl)
					.header("Set-Cookie", "CASTGC=deleted; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT")
					.build();
		}

		return Response
				.ok()
				.header("Set-Cookie", "CASTGC=deleted; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT")
				.build();
	}

	private NameIDType createNameId() {
		oasis.names.tc.saml._2_0.assertion.ObjectFactory objectFactory = new oasis.names.tc.saml._2_0.assertion.ObjectFactory();
		NameIDType nameId = objectFactory.createNameIDType();
		nameId.setValue("@NOT_USED@");
		return nameId;
	}

	private void logout(Service service) throws Exception {
		 JAXBElement<LogoutRequestType> element = createLogoutRequest(service);
		
		 StringWriter writer = new StringWriter();
		 JAXBContext context = JAXBContext.newInstance(LogoutRequestType.class);
		 Marshaller m = context.createMarshaller();
		 m.marshal(element, writer);
		 String xml = writer.toString();

		URL url = new URL(service.getServiceId());

		sendLogoutHttpMessage(url, xml);
	}

	private JAXBElement<LogoutRequestType> createLogoutRequest(Service service)
			throws Exception {
		oasis.names.tc.saml._2_0.protocol.ObjectFactory objectFactory = new oasis.names.tc.saml._2_0.protocol.ObjectFactory();

		String ticketId = generateTicketId();
		String logoutRequestTicketId = "LR-" + ticketId;

		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(new Date());
		XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);

		LogoutRequestType logoutReqType = objectFactory.createLogoutRequestType();
		logoutReqType.setID(logoutRequestTicketId);
		logoutReqType.setVersion("2.0");
		logoutReqType.setIssueInstant(xmlCalendar);
		logoutReqType.setNameID(createNameId());
		logoutReqType.getSessionIndex().add(service.getServiceTicketId());

		return objectFactory.createLogoutRequest(logoutReqType);
	}

	private void sendLogoutHttpMessage(URL url, String message) throws Exception {
		final String contentType = MediaType.APPLICATION_FORM_URLENCODED_TYPE.toString();

		final HttpPost request = new HttpPost(url.toURI());
		request.addHeader("Content-Type", contentType);
		
		message = "logoutRequest=" + URLEncoder.encode(message, "UTF-8"); 

		final StringEntity entity = new StringEntity(message, ContentType.create(contentType));
		request.setEntity(entity);

		try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(request)) {
		}
	}
}
