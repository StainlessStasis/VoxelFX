# VoxelFX

Animate client-side block & item display entities with keyframes, easing functions, and much more!<br>
Make cool VFX like this:
<video src="https://github.com/user-attachments/assets/272ee22a-42d3-4741-9064-bcfc3675386b" autoplay loop muted playsinline width="100%"></video>

# Features

### Animated Block & Item Displays
Animate block and item displays separately or together.

<details>
<summary><b>View Demo</b></summary>
<br>
<img width="1080" height="608" alt="blocks_and_items" src="https://github.com/user-attachments/assets/38d90e3e-2af5-49f4-81d6-7a187c82585c" />
</details>

### Keyframes & Easings
Use keyframes with easings to make super smooth animations for any movement you want. Reference [easings.net](https://easings.net/) to visualize easings, or test them out in game.

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

### Per-Frame Modifiers
Apply custom per-frame modifiers on top of the keyframed values, for effects like wobble, sway, or pulsing that aren't easily expressed as keyframes alone.<br>
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

# Limitations
NeoForge 26.1.2+<br>
This mod is client-only, meaning all effects will have to be triggered with your own code. For instance, sending a packet from server -> client, telling the client to play that animation.<br>
Animations are designed to be purely visual - do not try to alter the game state using them.<br><br>
Also, due to the clent-only nature of the mod, display entities do not natively support being saved to the world/persisting animations. You'd have to write your own system for that.

# General Use:
VoxelFX has some commands that can be run with `/voxelfx`. Most notably, the `demo` command can be used to run built-in demo animations. 
Also important - the `clear` command will clear out the entity cache, removing all VFX entities in case anything breaks.<br>
You can also pause, resume, stop, or set the play speed of all VFX entities.
*This is not a global state - it applies only to the entities that exist when the command is run.*

# Documentation
See the [wiki](https://github.com/StainlessStasis/VoxelFX/wiki/Getting-Started).
