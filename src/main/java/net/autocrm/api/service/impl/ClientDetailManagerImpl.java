package net.autocrm.api.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.autocrm.api.model.ClientDetails;
import net.autocrm.api.repository.ClientDetailsRepo;
import net.autocrm.api.service.ClientDetailManager;

@Service("clientMgr")
public class ClientDetailManagerImpl implements ClientDetailManager {

	@Autowired ClientDetailsRepo repo;

	@Override
	public List<ClientDetails> list() {
		return repo.findAll();
	}

	@Override
	public ClientDetails save( ClientDetails bean ) {
		return repo.save( bean );
	}
}
