package com.kurento.kmf.repository;

import java.util.List;

public interface Repository {

	RepositoryItem findRepositoryItemById(String id);

	RepositoryItem createRepositoryItem();

	RepositoryItem createRepositoryItem(String id);

	List<RepositoryItem> findRepositoryItemsByAttValue(String attributeName,
			String value);

	/**
	 * Perl compatible regular expressions (PCRE). For Mongo:
	 * http://docs.mongodb.org/manual/reference/operator/query/regex/
	 * 
	 * @param attributeName
	 * @param regex
	 * @return
	 */
	List<RepositoryItem> findRepositoryItemsByAttRegex(String attributeName,
			String regex);

	void remove(RepositoryItem item);

}
