package io.github.eisop.oopsie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;

public class OopsieTypeCache {

    private final OopsieAnnotatedTypeFactory atypeFactory;

    private final Map<AnnotationMirror, List<OopsieType>> inTypeCache;
    private final Map<AnnotationMirror, List<OopsieType>> outTypeCache;

    OopsieTypeCache(OopsieAnnotatedTypeFactory atypeFactory) {
        this.atypeFactory = atypeFactory;
        inTypeCache = new HashMap<>();
        outTypeCache = new HashMap<>();
    }

    List<OopsieType> getInTypes(AnnotationMirror annotation) {
        if (inTypeCache.containsKey(annotation)) {
            return inTypeCache.get(annotation);
        }
        List<String> inElement = atypeFactory.getInElement(annotation);
        List<OopsieType> inTypes =
                inElement.stream().map(OopsieType::fromAnnotationString).toList();
        inTypeCache.put(annotation, inTypes);
        return inTypes;
    }

    List<OopsieType> getOutTypes(AnnotationMirror annotation) {
        if (outTypeCache.containsKey(annotation)) {
            return outTypeCache.get(annotation);
        }
        List<String> outElement = atypeFactory.getOutElement(annotation);
        List<OopsieType> inTypes =
                outElement.stream().map(OopsieType::fromAnnotationString).toList();
        outTypeCache.put(annotation, inTypes);
        return inTypes;
    }
}
