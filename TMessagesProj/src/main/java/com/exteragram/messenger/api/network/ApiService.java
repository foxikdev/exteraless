package com.exteragram.messenger.api.network;

import com.exteragram.messenger.api.dto.ProfileDTO;

import java.util.List;

import kotlin.coroutines.Continuation;

/** Name-compatible service marker; requests are orchestrated by ApiController. */
public interface ApiService {

    default Object getAllProfiles(String etag, Continuation<? super Object> continuation) { return null; }

    default Object getUpdates(String since, String etag, Continuation<? super Object> continuation) { return null; }

    default Object getProfile(long userId, Continuation<? super ProfileDTO> continuation) { return null; }
}
