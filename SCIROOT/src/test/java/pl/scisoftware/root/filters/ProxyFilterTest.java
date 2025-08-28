package pl.scisoftware.root.filters;

import junit.framework.TestCase;

public class ProxyFilterTest extends TestCase {

	public void testFilter() {
		String uri = "/bawprd-p01.local.umed.pl:9443/teamworks/tm_process_finished.lsw";
		String rootContextsStr = "/bawprd-p01.local.umed.pl,/bpm.umed.pl";
		ProxyFilter proxyFilter = new ProxyFilter();
		proxyFilter.setRootContexts(rootContextsStr);
		assertNotNull(proxyFilter.getRootContexts());

		assertEquals("/teamworks/tm_process_finished.lsw", proxyFilter.checkURI(uri));

	}

}
