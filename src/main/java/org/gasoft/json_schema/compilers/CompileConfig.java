package org.gasoft.json_schema.compilers;

import org.gasoft.json_schema.IExternalResolver;
import org.gasoft.json_schema.IRegexPredicateFactory;
import org.gasoft.json_schema.common.regex.RegexFactory;
import org.gasoft.json_schema.loaders.IResourceLoader;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class CompileConfig {

    private boolean formatEnabled = false;
    private boolean allowTreatAsArray = false;
    private IRegexPredicateFactory regexpFactory = RegexFactory.jdk();
    private IExternalResolver externalSchemaResolver;
    private final List<IResourceLoader> resourceLoaders = new ArrayList<>();
    private Scheduler scheduler = Schedulers.fromExecutorService(Executors.newVirtualThreadPerTaskExecutor());

    public boolean isFormatEnabled() {
        return formatEnabled;
    }

    public CompileConfig setFormatEnabled(boolean formatEnabled) {
        this.formatEnabled = formatEnabled;
        return this;
    }

    public CompileConfig setRegexpFactory(IRegexPredicateFactory regexpFactory) {
        if(regexpFactory != null) {
            this.regexpFactory = regexpFactory;
        }
        return this;
    }

    public IRegexPredicateFactory getRegexpFactory() {
        return regexpFactory;
    }

    public CompileConfig addResourceLoader(IResourceLoader resourceLoader) {
        resourceLoaders.add(resourceLoader);
        return this;
    }

    public CompileConfig addResourceLoaders(Iterable<IResourceLoader> resourceLoaders) {
        resourceLoaders.forEach(this::addResourceLoader);
        return this;
    }

    public List<IResourceLoader> getResourceLoaders() {
        return resourceLoaders;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public CompileConfig setExternalSchemaResolver(IExternalResolver externalSchemaResolver) {
        this.externalSchemaResolver = externalSchemaResolver;
        return this;
    }

    public IExternalResolver getExternalSchemaResolver() {
        return externalSchemaResolver;
    }

    public CompileConfig setAllowTreatAsArray(boolean allowTreatAsArray) {
        this.allowTreatAsArray = allowTreatAsArray;
        return this;
    }

    public boolean isAllowTreatAsArray(){
        return allowTreatAsArray;
    }

    public CompileConfig setScheduler(Scheduler scheduler) {
        if(scheduler != null) {
            this.scheduler = scheduler;
        }
        return this;
    }
}
