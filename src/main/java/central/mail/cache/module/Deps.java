package central.mail.cache.module;

import central.mail.cache.CacheFacade;
import central.mail.cache.ICacheFacade;
import central.mail.cache.business.ICacheBusiness;
import central.mail.cache.impl.business.CacheBusiness;
import com.google.inject.AbstractModule;

public class Deps extends AbstractModule {
    @Override
    protected void configure() {
        this.bind(ICacheBusiness.class).to(CacheBusiness.class);
        this.bind(ICacheFacade.class).to(CacheFacade.class);
    }
}
