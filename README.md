# Block Display Animator

Animate block display entities to make cool VFX.

## Features

### Keyframes & Easings
Animate properties like translation, scale, rotation, and more using simple keyframes. Every easing function from [easings.net](https://easings.net/) is built in, and you can register your own custom easing functions.

### Overlays
Tint any block (including weird shapes like lecterns) any color. Intensity controls how opaque the tint is, and overlays support keyframes just like every other property.

### Animation Queueing
Queue animations to play one after another, or override the current animation immediately. Animations can inherit properties from whichever animation played before them to reduce boilerplate.

### Looping & Callbacks
Loop animations a fixed number of times or infinitely. Hook into `onStart`, `onEnd`, `onLoop`, and `onKeyframeReached` callbacks to trigger logic at any point in an animation's lifecycle.

### Per-Tick Modifiers
Apply custom per-tick modifiers to translation, scale, rotation, and overlay color/intensity on top of the keyframed values, for effects like wobble, sway, or pulsing that aren't easily expressed as keyframes alone.<br><br>
*Note: These should be scaled by the context's `interpolatedTicks` so everything stays consistent with any framerate.*

### Builder API
A fluent builder lets you construct animations declaratively, chaining translation, scale, rotation, overlay, and block state channels together with minimal boilerplate.
