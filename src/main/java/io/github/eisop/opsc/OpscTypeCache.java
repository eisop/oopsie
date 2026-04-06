package io.github.eisop.opsc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;

public class OpscTypeCache {

    private final OpsAnnotatedTypeFactory atypeFactory;

    private final Map<AnnotationMirror, List<OpscType>> inTypeCache;
    private final Map<AnnotationMirror, List<OpscType>> outTypeCache;

    OpscTypeCache(OpsAnnotatedTypeFactory atypeFactory) {
        this.atypeFactory = atypeFactory;
        inTypeCache = new HashMap<>();
        outTypeCache = new HashMap<>();
    }

    List<OpscType> getInTypes(AnnotationMirror annotation) {
        if (inTypeCache.containsKey(annotation)) {
            return inTypeCache.get(annotation);
        }
        List<String> inElement = atypeFactory.getInElement(annotation);
        List<OpscType> inTypes = inElement.stream().map(OpscType::fromAnnotationString).toList();
        inTypeCache.put(annotation, inTypes);
        return inTypes;
    }

    List<OpscType> getOutTypes(AnnotationMirror annotation) {
        if (outTypeCache.containsKey(annotation)) {
            return outTypeCache.get(annotation);
        }
        List<String> outElement = atypeFactory.getOutElement(annotation);
        List<OpscType> inTypes = outElement.stream().map(OpscType::fromAnnotationString).toList();
        outTypeCache.put(annotation, inTypes);
        return inTypes;
    }
}
