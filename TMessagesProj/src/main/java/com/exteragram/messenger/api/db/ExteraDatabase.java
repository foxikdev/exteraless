package com.exteragram.messenger.api.db;

/** Compatibility facade for the Room database singleton used by exteraGram plugins. */
public final class ExteraDatabase {
    public static final ExteraDatabase INSTANCE = new ExteraDatabase();

    private ExteraDatabase() {
    }

    public ExteraDatabase getInstance() {
        return this;
    }

    public ProfileDao profileDao() {
        return ProfileStore.INSTANCE;
    }
}
