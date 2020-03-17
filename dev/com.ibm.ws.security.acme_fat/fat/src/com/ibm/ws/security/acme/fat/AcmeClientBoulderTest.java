/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.fat;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.testcontainers.Testcontainers;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.BoulderContainer;
import com.ibm.ws.security.acme.docker.CAContainer;
import com.ibm.ws.security.acme.AcmeCertificate;
import com.ibm.ws.security.acme.internal.AcmeClient;
import com.ibm.ws.security.acme.internal.CSROptions;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;
import com.ibm.ws.security.acme.utils.HttpChallengeServer;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * FAT for the {@link AcmeClient} class. This sets up a Boulder certificate
 * authority which includes a mock DNS server.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AcmeClientBoulderTest {

	private static final String TEST_DOMAIN_1 = "domain1.com";
	private static final String TEST_DOMAIN_2 = "domain2.com";
	private static final String TEST_DOMAIN_3 = "domain3.com";
	private static final String TEST_DOMAIN_4 = "domain4.com";

	private static String TRUSTSTORE_FILE;
	private static String FILE_ACCOUNT_KEY;
	private static String FILE_DOMAIN_KEY;
	private static final String TRUSTSTORE_PASSWORD = "password";
	private static X509Certificate boulderIntermediateCertificate = null;
	private static HttpChallengeServer challengeServer = null;
	private static final String FILE_MINICA_PEM = "lib/LibertyFATTestFiles/test-ca2.pem";


	public static CAContainer boulder = null;
	

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	static {

	}

	/**
	 * We need to start up the containers in an orderly fashion so that we can
	 * pass the IP address of the DNS server to the Boulder server.
	 * @throws IOException 
	 */
	@BeforeClass
	public static void beforeClass() throws IOException {
		final String METHOD_NAME = "beforeClass()";

		String os = System.getProperty("os.name").toLowerCase();
		Assume.assumeTrue(!os.startsWith("z/os"));

		/*
		 * Need to expose the HTTP port that is used to answer the HTTP-01
		 * challenge.
		 */
		Testcontainers.exposeHostPorts(BoulderContainer.HTTP_PORT);

		/*
		 * Start a simple HTTP server to respond to challenges.
		 */
		challengeServer = new HttpChallengeServer(BoulderContainer.HTTP_PORT);
		challengeServer.start();

		/*
		 * Startup the boulder server.
		 */
		boulder = new BoulderContainer();
		boulder.start();

		Log.info(FATSuite.class, METHOD_NAME, "Boulder ContainerIpAddress: " + boulder.getContainerIpAddress());
		Log.info(FATSuite.class, METHOD_NAME, "Boulder DockerImageName:    " + boulder.getDockerImageName());
		Log.info(FATSuite.class, METHOD_NAME, "Boulder ContainerInfo:      " + boulder.getContainerInfo());
		
		try {
			/*
			 * Create temp files that we can use to create file paths.
			 */
			File truststore = File.createTempFile("truststore", "jks");
			File accountKey = File.createTempFile("accountKey", "pem");
			File domainKey = File.createTempFile("domainKey", "pem");
			TRUSTSTORE_FILE = truststore.getAbsolutePath();
			FILE_ACCOUNT_KEY = accountKey.getAbsolutePath();
			FILE_DOMAIN_KEY = domainKey.getAbsolutePath();

			/*
			 * Delete them so we can generate them in the test.
			 */
			truststore.delete();
			accountKey.delete();
			domainKey.delete();

			/*
			 * Delete the keys on exit.
			 */
			truststore.deleteOnExit();
			accountKey.deleteOnExit();
			domainKey.deleteOnExit();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		/*
		 * Download the ACME CA's intermediate certificate and convert it from a
		 * PEM to an X.509 certificate and install it in a trust store.
		 */
		try {
			/*
			 * Retrieve the intermediate certificate that will be used to sign
			 * any generated certificates. This is re-generated on each run.
			 * 
			 * Currently, this test doesn't require this certificate, but this
			 * is just an example of how to do so for *real* tests that will
			 * verify that the generated certificate is actually signed by the
			 * root and intermediate certificates.
			 */
			boulderIntermediateCertificate = AcmeFatUtils
					.getX509Certificate(new FileInputStream(new File(FILE_MINICA_PEM)));
			Log.info(AcmeClientBoulderTest.class, "<cinit>",
					"Boulder Intermediate Cert: " + String.valueOf(boulderIntermediateCertificate));

			/*
			 * Get the certificate generated by miniCA for the ACME HTTPS API.
			 * This is static and required to be used to talk to ACME over
			 * HTTPS.
			 */
			Certificate acmeHttpsCert = AcmeFatUtils.getX509Certificate(new FileInputStream(new File(FILE_MINICA_PEM)));
			Log.info(AcmeClientBoulderTest.class, "<cinit>", "ACME HTTPS Cert: " + String.valueOf(acmeHttpsCert));

			/*
			 * Write it to a JKS.
			 */
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(null, null);
			ks.setCertificateEntry("acme-https", acmeHttpsCert);
			ks.store(new FileOutputStream(TRUSTSTORE_FILE), TRUSTSTORE_PASSWORD.toCharArray());
		} catch (Exception e) {
			Log.error(AcmeClientBoulderTest.class, "<cinit>", e);
		}
		
	}

	@AfterClass
	public static void afterClass() throws InterruptedException {
		if (boulder != null) {
			boulder.stop();
		}
		if (challengeServer != null) {
			challengeServer.stop();
			challengeServer = null;
		}
	}

	@Before
	public void beforeTest() throws Exception {

		/*
		 * Setup the Mock DNS server to redirect requests to the test domains to
		 * this client.
		 */
		for (String domain : new String[] { TEST_DOMAIN_1, TEST_DOMAIN_2, TEST_DOMAIN_3, TEST_DOMAIN_4 }) {
			/*
			 * Disable the IPv6 responses for this domain. The Boulder CA server
			 * responds on AAAA (IPv6) responses before A (IPv4) responses, and
			 * we don't currently have the testcontainer host's IPv6 address.
			 */
			boulder.addARecord(domain, boulder.getClientHost());
			boulder.addAAAARecord(domain, "");
		}
	}

	/**
	 * Fetch a certificate for a single domain.
	 * 
	 * @throws Exception
	 */
	@Test
	public void fetchCertificate_SingleDomain() throws Exception {

		/*
		 * Create an AcmeService to test.
		 */
		AcmeClient acmeClient = new AcmeClient(boulder.getAcmeDirectoryURI(), FILE_ACCOUNT_KEY, FILE_DOMAIN_KEY, null);
		acmeClient.setAcceptTos(true);

		/*
		 * Link the new client to the challenge response server.
		 */
		challengeServer.setAcmeClient(acmeClient);

		/*
		 * Get the certificate from the ACME CA server.
		 */
		AcmeCertificate newCertificate = acmeClient
				.fetchCertificate(new CSROptions(Arrays.asList(new String[] { TEST_DOMAIN_1 })));

		/*
		 * Verify that the certificate returned from the ACME CA is signed by
		 * the ACME CA's intermediate certificate public key.
		 */
		X509Certificate cert = newCertificate.getCertificate();
		newCertificate.getCertificate().verify(boulderIntermediateCertificate.getPublicKey());
		assertEquals("CN=" + TEST_DOMAIN_1, newCertificate.getCertificate().getSubjectDN().getName());
	}

	/**
	 * Test fetching a certificate form the ACME CA for several domains.
	 * 
	 * @throws Exception
	 */
	@Test
	public void fetchCertificate_MultiDomain_Success() throws Exception {

		/*
		 * Create an AcmeService to test.
		 */
		AcmeClient acmeClient = new AcmeClient(boulder.getAcmeDirectoryURI(), FILE_ACCOUNT_KEY, FILE_DOMAIN_KEY, null);
		acmeClient.setAcceptTos(true);

		/*
		 * Link the new client to the challenge response server.
		 */
		challengeServer.setAcmeClient(acmeClient);

		/*
		 * Get the certificate from the ACME CA server.
		 */
		AcmeCertificate newCertificate = acmeClient.fetchCertificate(new CSROptions(
				Arrays.asList(new String[] { TEST_DOMAIN_1, TEST_DOMAIN_2, TEST_DOMAIN_3, TEST_DOMAIN_4 })));

		/*
		 * Verify that the certificate returned from the ACME CA is signed by
		 * the ACME CA's intermediate certificate public key.
		 * 
		 * When processing multiple domains, the subject DN will be that of the
		 * first domain.
		 */
		newCertificate.getCertificate().verify(boulderIntermediateCertificate.getPublicKey());
		assertEquals("CN=" + TEST_DOMAIN_1, newCertificate.getCertificate().getSubjectDN().getName());
	}

	/**
	 * Verify that we can revoke a fetched certificate.
	 * 
	 * @throws Exception
	 */
	//@Test
	public void fetchCertificate_Revoke() throws Exception {
		/*
		 * Create an AcmeService to test.
		 */
		AcmeClient acmeClient = new AcmeClient(boulder.getAcmeDirectoryURI(), FILE_ACCOUNT_KEY, FILE_DOMAIN_KEY, null);
		acmeClient.setAcceptTos(true);

		/*
		 * Link the new client to the challenge response server.
		 */
		challengeServer.setAcmeClient(acmeClient);

		/*
		 * Get the certificate from the ACME CA server.
		 */
		AcmeCertificate newCertificate = acmeClient
				.fetchCertificate(new CSROptions(Arrays.asList(new String[] { TEST_DOMAIN_1 })));

		/*
		 * The certificate should be valid.
		 */
		assertEquals("The new certificate should be valid.", "Valid",
				boulder.getAcmeCertificateStatus(newCertificate.getCertificate()));

		/*
		 * Revoke the certificate.
		 */
		acmeClient.revoke(newCertificate.getCertificate());

		/*
		 * The certificate should now be revoked.
		 */
		assertEquals("The new certificate should be revoked.", "Revoked",
				boulder.getAcmeCertificateStatus(newCertificate.getCertificate()));
	}
}
