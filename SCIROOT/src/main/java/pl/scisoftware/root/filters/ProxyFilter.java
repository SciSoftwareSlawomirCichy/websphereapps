package pl.scisoftware.root.filters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.FileCopyUtils;

import pl.scisoftware.root.utils.TraceUtils;

public class ProxyFilter implements Filter {

	private static final String DISPLAY_CLASSNAME = TraceUtils.createDisplayClazzName(ProxyFilter.class);
	private static final Logger trcLogger = TraceUtils
			.createTraceLogger("filters." + ProxyFilter.class.getSimpleName());

	protected Set<String> rootContexts;

	protected String targetBaseUrl;

	public void init(FilterConfig filterConfig) throws ServletException {
		String rootContextsStr = filterConfig.getInitParameter("rootContexts");
		setRootContexts(rootContextsStr);
		this.targetBaseUrl = filterConfig.getInitParameter("targetBaseUrl");
	}

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		final String METHOD_NAME = "[SCIROOT].doFilter";
		if (trcLogger.isLoggable(Level.FINE)) {
			trcLogger.entering(DISPLAY_CLASSNAME, METHOD_NAME);
		}

		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		String requestURI = request.getRequestURI();

		if (trcLogger.isLoggable(Level.FINE)) {
			trcLogger.logp(Level.FINE, DISPLAY_CLASSNAME, METHOD_NAME,
					String.format("-->requestURI: '%s'", requestURI));
		}

		if (StringUtils.isNotBlank(requestURI)) {
			String newRequestURI = checkURI(requestURI);
			if (!requestURI.equals(newRequestURI)) {
				trcLogger.logp(Level.INFO, DISPLAY_CLASSNAME, METHOD_NAME,
						String.format("--> Request URI is changed from `%s` to `%s`", requestURI, newRequestURI));
				sendRedirect(request, response, newRequestURI);
				/* Zakończ przetwarzanie w filtrze */
				return;
			}
		}
		filterChain.doFilter(servletRequest, servletResponse);
		if (trcLogger.isLoggable(Level.FINE)) {
			trcLogger.exiting(DISPLAY_CLASSNAME, METHOD_NAME);
		}
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void proxyUrl(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String newRequestURI)
			throws IOException, ServletException {
		final String METHOD_NAME = "[SCIROOT].proxyUrl";
		if (trcLogger.isLoggable(Level.FINE)) {
			trcLogger.entering(DISPLAY_CLASSNAME, METHOD_NAME);
		}

		String targetUrl = this.targetBaseUrl + newRequestURI;
		StringBuilder targetUrlBuilder = new StringBuilder(targetUrl);
		String queryString = httpRequest.getQueryString();
		if (queryString != null && !queryString.isEmpty()) {
			targetUrlBuilder.append("?").append(queryString);
		}
		if (trcLogger.isLoggable(Level.FINE)) {
			trcLogger.logp(Level.FINE, DISPLAY_CLASSNAME, METHOD_NAME,
					String.format("-->START for targetUrl: %s", targetUrlBuilder.toString()));
		}

		try {
			URL url = new URL(targetUrlBuilder.toString());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			/* set timeout to 1000 seconds */
			connection.setConnectTimeout(1000 * 1000);
			/* Skopiuj metodę HTTP z oryginalnego żądania */
			connection.setRequestMethod(httpRequest.getMethod());

			/* Skopiuj nagłówki z oryginalnego żądania do nowego */
			Enumeration<String> headerNames = httpRequest.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				connection.setRequestProperty(headerName, httpRequest.getHeader(headerName));
			}

			/* Skopiuj ciało żądania, jeśli istnieje */
			if ("POST".equalsIgnoreCase(httpRequest.getMethod()) || "PUT".equalsIgnoreCase(httpRequest.getMethod())) {
				connection.setDoOutput(true);
				try (InputStream requestInput = httpRequest.getInputStream();
						OutputStream proxyOutput = connection.getOutputStream()) {
					FileCopyUtils.copy(requestInput, proxyOutput);
				}
			}

			/* Połącz i odczytaj odpowiedź */
			int status = connection.getResponseCode();
			httpResponse.setStatus(status);
			if (trcLogger.isLoggable(Level.FINE)) {
				trcLogger.logp(Level.FINE, DISPLAY_CLASSNAME, METHOD_NAME,
						String.format("-->Response status: %d", status));
			}

			/* Skopiuj nagłówki odpowiedzi */
			connection.getHeaderFields().forEach((key, valueList) -> {
				if (key != null) {
					valueList.forEach(value -> httpResponse.addHeader(key, value));
				}
			});

			/* Skopiuj ciało odpowiedzi */
			try (InputStream proxyInput = connection.getInputStream();
					OutputStream responseOutput = httpResponse.getOutputStream()) {
				FileCopyUtils.copy(proxyInput, responseOutput);
			}
		} catch (Exception e) {
			trcLogger.logp(Level.SEVERE, DISPLAY_CLASSNAME, METHOD_NAME, String.format("-->[%s] for targetUrl: %s: %s",
					e.getClass().getSimpleName(), targetUrlBuilder.toString(), e.getMessage()));
			throw new ServletException(String.format("Proxy failed for request URI: %s", httpRequest.getRequestURI()),
					e);
		}
		if (trcLogger.isLoggable(Level.FINE)) {
			trcLogger.logp(Level.FINE, DISPLAY_CLASSNAME, METHOD_NAME,
					String.format("-->END for targetUrl: %s", targetUrlBuilder.toString()));
		}
		if (trcLogger.isLoggable(Level.FINE)) {
			trcLogger.exiting(DISPLAY_CLASSNAME, METHOD_NAME);
		}
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void sendRedirect(HttpServletRequest request, HttpServletResponse response, String newRequestURI)
			throws IOException {
		final String METHOD_NAME = "[SCIROOT].sendRedirect";
		if (trcLogger.isLoggable(Level.FINE)) {
			trcLogger.entering(DISPLAY_CLASSNAME, METHOD_NAME);
		}

		Map<String, String[]> params = request.getParameterMap();
		StringBuilder newUrlBuilder = new StringBuilder(newRequestURI);
		if (!params.isEmpty()) {
			newUrlBuilder.append("?");
			for (Map.Entry<String, String[]> entry : params.entrySet()) {
				String paramName = entry.getKey();
				String[] paramValues = entry.getValue();
				/* Enkoduj nazwę parametru */
				String encodedParamName = URLEncoder.encode(paramName, "UTF-8");
				for (String paramValue : paramValues) {
					/* Enkoduj wartość parametru */
					String encodedParamValue = URLEncoder.encode(paramValue, "UTF-8");
					newUrlBuilder.append(encodedParamName).append("=").append(encodedParamValue).append("&");
				}
			}
			/* Usuń ostatnie '&' */
			newUrlBuilder.deleteCharAt(newUrlBuilder.length() - 1);
		}
		response.sendRedirect(newUrlBuilder.toString());
		if (trcLogger.isLoggable(Level.FINE)) {
			trcLogger.logp(Level.FINE, DISPLAY_CLASSNAME, METHOD_NAME,
					String.format("-->newUrl: %s", newUrlBuilder.toString()));
		}
		if (trcLogger.isLoggable(Level.FINE)) {
			trcLogger.exiting(DISPLAY_CLASSNAME, METHOD_NAME);
		}
	}

	public void destroy() {
		this.rootContexts = null;
		this.targetBaseUrl = null;
	}

	public String checkURI(final String requestURI) {
		final String METHOD_NAME = "[SCIROOT].checkURI";
		if (trcLogger.isLoggable(Level.FINE)) {
			trcLogger.entering(DISPLAY_CLASSNAME, METHOD_NAME);
		}
		String newURI = requestURI;
		if (this.rootContexts != null && !this.rootContexts.isEmpty()) {
			/* Znajdź indeks drugiego ukośnika, który oddziela hosta od ścieżki */
			int secondSlashIndex = requestURI.indexOf('/', requestURI.indexOf('/') + 1);
			for (String contextPrefix : this.rootContexts) {
				if (trcLogger.isLoggable(Level.FINE)) {
					trcLogger.logp(Level.FINE, DISPLAY_CLASSNAME, METHOD_NAME, String.format(
							"Context prefix: %s, URI Starts? %s", contextPrefix, requestURI.startsWith(contextPrefix)));
				}
				if (requestURI.startsWith(contextPrefix)) {
					newURI = requestURI.substring(secondSlashIndex);
					break;
				}
			}
		}
		if (trcLogger.isLoggable(Level.FINE)) {
			trcLogger.exiting(DISPLAY_CLASSNAME, METHOD_NAME);
		}
		return newURI;
	}

	/**
	 * @param rootContexts the rootContexts to set
	 */
	public void setRootContexts(String rootContextsStr) {
		if (StringUtils.isNotBlank(rootContextsStr)) {
			this.rootContexts = new HashSet<>(Arrays.asList(rootContextsStr.split("\\,")));
		}
	}

	/**
	 * @return the rootContexts
	 */
	public Set<String> getRootContexts() {
		return rootContexts;
	}

}
