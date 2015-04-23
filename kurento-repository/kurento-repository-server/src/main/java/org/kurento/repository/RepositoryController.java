package org.kurento.repository;

import java.util.Map;

import org.kurento.repository.service.pojo.RepositoryItemStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/repo/item")
public class RepositoryController {

	@Autowired
	private RepositoryService repoService;

	@RequestMapping(method = RequestMethod.POST, produces = "application/json")
	public RepositoryItemStore createRepositoryItem(
			@RequestBody(required = false) Map<String, String> metadata) {
		return repoService.createRepositoryItem(metadata);
	}

	@RequestMapping(method = RequestMethod.GET, produces = "text/plain", value = "/{itemId}/read")
	public String getReadEndpoint(@PathVariable("itemId") String itemId) {
		return "\"" + repoService.getReadEndpoint(itemId) + "\"";
	}

	// TODO should exist?
	@RequestMapping("/{itemId}/write")
	public String getWriteEndpoint(@PathVariable("itemId") String itemId) {
		return repoService.getWriteEndpoint(itemId);
	}

	// TODO add
	// * find by metadata
	// * get metadata
	// * update(replace) metadata
}
