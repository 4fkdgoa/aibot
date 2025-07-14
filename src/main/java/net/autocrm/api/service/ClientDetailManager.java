package net.autocrm.api.service;

import java.util.List;

import net.autocrm.api.model.ClientDetails;

public interface ClientDetailManager {

	List<ClientDetails> list();

	ClientDetails save(ClientDetails bean);

}
