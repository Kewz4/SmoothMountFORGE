package com.enderium.smoothmount.client.animation.definition;

import com.enderium.smoothmount.client.animation.IndexedAnimation;
import com.enderium.smoothmount.client.animation.TransformChannel;
import com.enderium.smoothmount.client.animation.TransformTrack;
import com.enderium.smoothmount.client.model.ModelPartChildren;
import net.minecraft.client.model.geom.ModelPart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelAnimation implements AnimationChannel {
    private final ModelAnimationDefinition definition;
    private final List<Entry> entryList;

    private ModelAnimation(ModelAnimationDefinition definition, List<Entry> entryList) {
        this.definition = definition;
        this.entryList = entryList;
    }

    public static ModelAnimation bake(ModelPart part, ModelAnimationDefinition definition) {
        List<Entry> entries = new ArrayList<>();
        Map<String, ModelPart> lookup = new HashMap<>();
        buildLookup(lookup, "root", part);

        Map<String, TransformTrack> map = definition.boneAnimations();
        for (var entry : map.entrySet()) {
            ModelPart find = lookup.get(entry.getKey());
            if (find == null) continue;
            entries.add(new Entry(find, entry.getValue().createChannel()));
        }
        return new ModelAnimation(definition, entries);
    }

    private static void buildLookup(Map<String, ModelPart> map, String name, ModelPart part) {
        map.put(name, part);
        for (var entry : ((ModelPartChildren)(Object) part).children().entrySet()) {
            buildLookup(map, entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void update(float time, int index, int[] gIndices) {
        for (Entry entry : entryList) {
            entry.update(time, index, gIndices);
            index += 3;
        }
    }

    @Override
    public int getIndexCount() {
        return definition.getIndexCount();
    }

    @Override
    public float lengthInSeconds() {
        return definition.lengthInSeconds();
    }

    @Override
    public boolean looping() {
        return definition.looping();
    }

    public ModelAnimationDefinition definition() {
        return definition;
    }

    private record Entry(ModelPart part, TransformChannel channel) implements IndexedAnimation {

        @Override
        public void update(float time, int index, int[] gIndices) {
            channel.update(time, index, gIndices);
            part.offsetPos(channel.position().currentValue);
            part.offsetRotation(channel.rotation().currentValue);
            part.offsetScale(channel.scale().currentValue);
        }

        @Override
        public int getIndexCount() {
            return channel.getIndexCount();
        }
    }
}
