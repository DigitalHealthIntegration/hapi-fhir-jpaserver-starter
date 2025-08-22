package interceptor;

import ca.uhn.fhir.jpa.starter.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;

@ExtendWith(MockitoExtension.class)
class SignatureInterceptorTest {

	@Mock
	private SignatureInterceptor signatureInterceptor;
	@Mock
	private Log logger;

	String signatureHeader = "bVu5ibK%2FDFSqUROLxZijqqM7yJ%2FT4UXYb%2F12kemt9wcwIpMBzj5NUwIEJBL%2Bkb6ypu8Q2quQBd0c90QULKoxyZ%2B0YSqcRTs7Z63Ia%2F75Pg1recD4Ow7%2FnlV7ba6Fbnmdw%2Bm3VxI7%2BbdUEFY0nVRIOUXvMEBUeBUUr2PB%2BaOKy2OYiENFcAf1KoyzClp6Ld1QOjj%2BKkVIg8IWACnGHx%2BWxRBWnAb55%2FrmYpjd2eLbQ68KoyH93ar13YERmOmvWFcnxPUxhdXpNHIjpyPFy9kwdVaY6vrHkrtu5aB1CfHQCfKqC90axN9JpWhGU%2F9YvX%2BmyW4TsOL09jz3YdL4evAk1A%3D%3D";
	String token = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJzeGpFTkx0VUdTTWtTTXNTOXF6WDh6VW8yZDE2M2xZSWNacWxfS2NnX2RRIn0.eyJleHAiOjE3MjA3NjgyMDEsImlhdCI6MTcyMDE2MzQwMSwiYXV0aF90aW1lIjoxNzIwMTYzMjg4LCJqdGkiOiIyODQwMDRjOC1jZTVhLTQzMDEtYTZmMS05YTBlOTlhOGRmNzMiLCJpc3MiOiJodHRwczovL25pZ2VyaWE0Lm9wZW5jYW1wYWlnbmxpbmsub3JnOjk0NDMvYXV0aC9yZWFsbXMvZmhpci1oYXBpIiwic3ViIjoiNTMxMmUyOGMtODk0OC00YjhmLWFlMzMtYTM5NTUyMDQ2NjQzIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZmhpci1oYXBpLXNlcnZlciIsInNlc3Npb25fc3RhdGUiOiI2ZWRiNWE3My04MDIxLTQzMWEtOWVlYy02YjVmNDY3MGIxOTciLCJhY3IiOiIwIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHBzOi8vbmlnZXJpYTQub3BlbmNhbXBhaWdubGluay5vcmc6ODA4NSIsImh0dHBzOi8vbmlnZXJpYTQub3BlbmNhbXBhaWdubGluay5vcmc6MzAwMCJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJkZWZhdWx0LXJvbGVzLWZoaXItaGFwaSIsInVtYV9hdXRob3JpemF0aW9uIl19LCJzY29wZSI6Im9wZW5pZCBvZmZsaW5lX2FjY2VzcyBwaG9uZSBlbWFpbCBwcm9maWxlIiwic2lkIjoiNmVkYjVhNzMtODAyMS00MzFhLTllZWMtNmI1ZjQ2NzBiMTk3IiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmFjdGl0aW9uZXJfaWQiOiI1MzdlYTc3ZS05NGJlLTRhZjctYjg2MC1hMzAwZWE2OTY5NWUiLCJwcmFjdGl0aW9uZXJfcm9sZV9pZCI6ImE0Y2M5OTE3LWQ2ODAtNDIxZC04MGQ2LWI0MGJiOTBjODk5OCIsIm5hbWUiOiJLYXJ0aGlrIFRlc3QiLCJwaG9uZV9udW1iZXIiOiIyMDU1NTIzMzIzMzQxMjM0IiwicHJlZmVycmVkX3VzZXJuYW1lIjoia2FydGhpa190ZXN0IiwiZ2l2ZW5fbmFtZSI6IkthcnRoaWsiLCJmYW1pbHlfbmFtZSI6IlRlc3QiLCJlbWFpbCI6ImthcnRoaWtfdGVzdEBpcHJkZ3JvdXAuY29tIn0.ebIG-dk9MQ8EHCpDh-4uD8oSWtv9DITLfT5VlB5pY3-XQoS0JBqyGLH3xp3xPf_MMvYBq1g0S5jDiJkVlFBxdrtEj0tUvMObGfV9YTyneg6geC1lpproruQE2bWYafFTX82RgAzj-JU7ZmBk7Ks7ffIU8vYm75Rfxt_Bea8DBNyHy-nDLWIc8jTqWzAD-SK3t5-SN9HIaULsTIQ-zb11vi9herH-HeQj5W7MVPF2S13bOHspbnJjDNYNAK1GEuDRILCY92C_T5_t1axZGRxBTqtIxsfdKKQT7tVdO_A_Y3Rmg10v1kkK_7f541OVfZVV2i_6NZcyEnN-9L9AUDDwqg";
	String timeStampHeader = "1720163552";
	String keyId = "APPCLIENT";

	@Test
	void testGetPublicKeyAndVerify_validCase() throws Exception {
		Field field = SignatureInterceptor.class.getDeclaredField("publicKeyCache");
		field.setAccessible(true);
		Map<String, byte[]> publicKeyCache = new HashMap<>();
		field.set(signatureInterceptor, publicKeyCache);
		AppProperties appPropertiesMock =  new AppProperties();
		// Add the absolute path of public key
		appPropertiesMock.setPk_file("D:\\anc-oyo\\data\\hapi\\android.txt");
		long currentTimestamp = Instant.now().getEpochSecond();
		long receivedTimeStamp = Long.parseLong(timeStampHeader);
		long timeDifference = Math.abs(currentTimestamp - receivedTimeStamp) + 100;
		appPropertiesMock.setApi_request_max_time(timeDifference);
		Whitebox.setInternalState(signatureInterceptor, appPropertiesMock);
		Whitebox.setInternalState(signatureInterceptor, "logger", logger);
		doCallRealMethod().when(signatureInterceptor).getPublicKeyAndVerify(anyString(),anyString(),anyString(),anyString());
		doCallRealMethod().when(signatureInterceptor).readPublicKeyFile(anyString());
		Boolean output = signatureInterceptor.getPublicKeyAndVerify(signatureHeader, token, timeStampHeader, keyId );
		assertEquals(true, output);
	}
}