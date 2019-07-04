package central.mail.cache.module;

import bee.error.BusinessException;
import bee.registry.Modulo;
import bee.registry.Registry;
import central.mail.cache.business.ICacheBusiness;
import com.google.inject.AbstractModule;

public class Cache extends Modulo {
    @Override
    public String getNombre() {
        return "CACHE";
    }

    @Override
    public AbstractModule getDependencies() {
        return new Deps();
    }

    @Override
    public void init() throws BusinessException {
        var cache = Registry.getInstance(ICacheBusiness.class);
        cache.init();
    }
}
