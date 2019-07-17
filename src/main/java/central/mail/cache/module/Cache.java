package central.mail.cache.module;


import central.mail.cache.business.ICacheBusiness;
import cognitivesolutions.error.BusinessException;
import cognitivesolutions.registry.Modulo;
import cognitivesolutions.registry.Registry;
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
