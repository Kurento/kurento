package com.kurento.kmf.repository.internal.repoimpl;

import com.kurento.kmf.repository.Repository;
import com.kurento.kmf.repository.internal.http.RepositoryHttpManager;

public interface RepositoryWithHttp extends Repository {

	public RepositoryHttpManager getRepositoryHttpManager();

}
