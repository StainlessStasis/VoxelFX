# Block Display Animator

Animate block display entities to make cool VFX, like this:
<video src="https://github.com/user-attachments/assets/272ee22a-42d3-4741-9064-bcfc3675386b" autoplay loop muted playsinline width="100%"></video>

## Features

### Animated Block & Item Displays
Animate blocks and items separately or together.

<details>
<summary><b>View Demo</b></summary>
<br>
<img width="1080" height="608" alt="blocks_and_items" src="https://github.com/user-attachments/assets/38d90e3e-2af5-49f4-81d6-7a187c82585c" />
</details>

### Keyframes & Easings
Every easing function from [easings.net](https://easings.net/) is built in, and you can register your own custom easing functions.

<details>
<summary><b>View Demo</b></summary>
<br>
<img width="1080" height="608" alt="keyframes_and_easings" src="https://github.com/user-attachments/assets/b9df9f06-3092-47c2-955e-5bbe10c2a41f"/>
</details>

### Overlays
Tint any block (including weird shapes like lecterns) any color. Intensity controls how opaque the tint is, and overlays support keyframes just like every other property.<br>
*Does not support items.*

<details>
<summary><b>View Demo</b></summary>
<br>
<img width="1080" height="608" alt="overlay" src="https://github.com/user-attachments/assets/11c5b6d9-0930-4702-b50a-a44834ecd323" />
</details>

### Animation Queueing & Inheritance
Queue animations to play one after another, or override the current animation immediately. Animations can inherit properties from whichever animation played before them to reduce boilerplate.

<details>
<summary><b>View Demo</b></summary>
<br>
<img width="1080" height="608" alt="queue_and_inheritance" src="https://github.com/user-attachments/assets/e5c44541-d7a7-4399-9f2a-f805eea9a4a8" />
</details>

### Looping & Callbacks
Loop animations a fixed number of times or infinitely. Hook into callbacks to trigger logic at any point in an animation's lifecycle.

<details>
<summary><b>View Demo</b></summary>
<br>
<img width="1080" height="608" alt="loops_and_callbacks" src="https://github.com/user-attachments/assets/dced5158-a772-4660-96a9-eb994bf59a54" />
</details>

### Per-Tick Modifiers
Apply custom per-tick modifiers on top of the keyframed values, for effects like wobble, sway, or pulsing that aren't easily expressed as keyframes alone.<br>
*Note: These should be scaled by the context's `interpolatedTicks` so everything stays consistent with any framerate.*

<details>
<summary><b>View Demo</b></summary>
<br>
<img width="1080" height="608" alt="per_tick_modifiers" src="https://github.com/user-attachments/assets/e05c1e3b-d92d-4bc9-b318-994a9e74ca42" />
</details>

### Entity Binding
Bind animations to follow entities with an optional offset. Can operate in either global or local space.<br>
*Showcased in above Nova Bomb video.*

### Builder API
A fluent builder that lets you construct animations declaratively, chaining translation, scale, rotation, overlay, and block/item channels with minimal boilerplate.
