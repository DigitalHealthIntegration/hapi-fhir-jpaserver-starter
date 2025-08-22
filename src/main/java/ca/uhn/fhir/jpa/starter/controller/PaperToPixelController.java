package ca.uhn.fhir.jpa.starter.controller;

import ca.uhn.fhir.jpa.starter.model.DocumentReviewPayload;
import ca.uhn.fhir.jpa.starter.service.PaperToPixelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Controller
@RequestMapping(value = "/iprd/p2p")
@CrossOrigin(origins = { "http://localhost:3000/", "http://testhost.dashoboard:3000/", "https://oclink.io/", "https://opencampaionlink.org/" }, maxAge = 3600, allowCredentials = "true")
public class PaperToPixelController {

	@Autowired
	PaperToPixelService paperToPixelService;

	@GetMapping("/{documentId}/details")
	public ResponseEntity<DocumentReviewPayload> getReviewData(@PathVariable("documentId") String documentId){
		DocumentReviewPayload payload = paperToPixelService.getDocumentReviewPayload(documentId);
		return ResponseEntity.ok(payload);
	}

	@GetMapping("/{documentId}/image")
	public ResponseEntity<Resource>  getImage(@PathVariable("documentId") String documentId) throws IOException {
		Resource imageResource = paperToPixelService.getDocumentImage(documentId);
		return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
			.body(imageResource);
	}
}
