package your.game

import com.dwursteisen.minigdx.scene.api.Scene
import com.dwursteisen.minigdx.scene.api.relation.Node
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.Component
import com.github.dwursteisen.minigdx.ecs.components.HorizontalAlignment
import com.github.dwursteisen.minigdx.ecs.components.ScriptComponent
import com.github.dwursteisen.minigdx.ecs.components.TextComponent
import com.github.dwursteisen.minigdx.ecs.components.gl.BoundingBox
import com.github.dwursteisen.minigdx.ecs.components.text.WaveEffect
import com.github.dwursteisen.minigdx.ecs.components.text.WriteText
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.entities.position
import com.github.dwursteisen.minigdx.ecs.physics.AABBCollisionResolver
import com.github.dwursteisen.minigdx.ecs.physics.RayResolver
import com.github.dwursteisen.minigdx.ecs.script.ScriptContext
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.Font
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.input.Key
import com.github.dwursteisen.minigdx.math.Vector3
import your.game.Movable.State.DIRECTION
import your.game.Movable.State.MOVING
import your.game.Movable.State.SELECTED
import your.game.Movable.State.WAIT
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.random.Random

class SmokeComponent(var ttl: Seconds = 0.5f + Random.nextFloat()) : Component
class SmokeParticuleComponent(var ttl: Seconds = 0.8f + Random.nextFloat()) : Component
class Movable(var state: State = WAIT, var z: Float = 0f, var x: Float = 0f) : Component {
    enum class State {
        WAIT, SELECTED, DIRECTION, MOVING
    }
}

class Wall : Component

class Instruction : Component
class Player : Component
class Target : Component

class SmokeSystem(private val model: Node, private val scene: Scene) : System(EntityQuery.of(SmokeComponent::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        val c = entity.get(SmokeComponent::class)
        c.ttl -= delta
        if (c.ttl < 0) {
            c.ttl = 0.5f + Random.nextFloat()
            entityFactory.createFromNode(model, scene).also {
                val (x, y, z) = entity.position.translation
                it.position.setLocalTranslation(x, y, z)
                it.add(SmokeParticuleComponent())
            }
        }
    }
}

class SmoleParticuleSystem : System(EntityQuery.Companion.of(SmokeParticuleComponent::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        val c = entity.get(SmokeParticuleComponent::class)
        entity.position.addLocalTranslation(y = 1f, delta = delta)
        c.ttl -= delta
        if (c.ttl < 0f) {
            entity.destroy()
        }
    }
}

class MovableSystem : System(EntityQuery.of(Movable::class)) {

    private var originY: Float = 0f
    private var time = 0f

    private val rayResolver = RayResolver()

    private val walls by interested(EntityQuery.of(Wall::class))

    private var changed = false

    override fun onGameStarted(engine: Engine) {
        val picked = entities.random()
        originY = picked.position.translation.y
        picked.get(Movable::class).state = SELECTED
    }

    override fun update(delta: Seconds) {
        time += delta
        super.update(delta)
        changed = false
    }

    override fun update(delta: Seconds, entity: Entity) {
        if (changed) {
            return
        }
        val state = entity.get(Movable::class).state
        when (state) {
            WAIT -> return
            SELECTED -> {
                entity.position.setLocalTranslation(y = originY + abs(cos(time * 4f) * 0.5f))
                val box = entity.get(BoundingBox::class).center
                if (input.isKeyJustPressed(Key.ARROW_LEFT)) {

                    val next = entities.filter { box.z < it.get(BoundingBox::class).center.z }
                        .minByOrNull { entity.position.translation.dist2(it.position.translation) }
                    next ?: return
                    entity.get(Movable::class).state = WAIT
                    next.get(Movable::class).state = SELECTED
                    changed = true
                    time = 0f
                    entity.position.setLocalTranslation(y = originY)
                } else if (input.isKeyJustPressed(Key.ARROW_RIGHT)) {
                    val next = entities.filter { box.z > it.get(BoundingBox::class).center.z }
                        .minByOrNull { entity.position.translation.dist2(it.position.translation) }
                    next ?: return
                    entity.get(Movable::class).state = WAIT
                    next.get(Movable::class).state = SELECTED
                    changed = true
                    time = 0f
                    entity.position.setLocalTranslation(y = originY)
                } else if (input.isKeyJustPressed(Key.ARROW_DOWN)) {
                    val next = entities.filter { box.x < it.get(BoundingBox::class).center.x }
                        .minByOrNull { entity.position.translation.dist2(it.position.translation) }
                    next ?: return
                    entity.get(Movable::class).state = WAIT
                    next.get(Movable::class).state = SELECTED
                    changed = true
                    time = 0f
                    entity.position.setLocalTranslation(y = originY)
                } else if (input.isKeyJustPressed(Key.ARROW_UP)) {
                    val next = entities.filter { box.x > it.get(BoundingBox::class).center.x }
                        .minByOrNull { entity.position.translation.dist2(it.position.translation) }
                    next ?: return
                    entity.get(Movable::class).state = WAIT
                    next.get(Movable::class).state = SELECTED
                    changed = true
                    time = 0f
                    entity.position.setLocalTranslation(y = originY)
                } else if (input.isKeyJustPressed(Key.ENTER) || input.isKeyJustPressed(Key.SPACE)) {
                    entity.get(Movable::class).state = DIRECTION
                }
            }
            DIRECTION -> {
                entity.position.setLocalTranslation(y = originY + 0.3f + abs(cos(time * 0.5f) * 0.1f))
                if (input.isKeyJustPressed(Key.ENTER) || input.isKeyJustPressed(Key.SPACE)) {
                    entity.get(Movable::class).state = SELECTED
                } else if (input.isKeyJustPressed(Key.ARROW_LEFT)) {
                    if (entity.position.rotation.y != 0.0f) {
                        return
                    }
                    entity.get(Movable::class).state = MOVING
                    val box = entity.get(BoundingBox::class)
                    val origin = Vector3(box.center.x, box.center.y, box.max.z)
                    val closest = (walls - entity).mapNotNull { other ->
                        val hit = rayResolver.intersectRayBounds(origin, Vector3(0, 0, 1), other)
                        hit
                    }.minByOrNull { it.dist2(origin) }
                    entity.get(Movable::class).x = 0f
                    entity.get(Movable::class).z = closest!!.z - origin.z
                    entity.add(ScriptComponent(
                        script = { moveObj(entity) }
                    ))
                } else if (input.isKeyJustPressed(Key.ARROW_RIGHT)) {
                    if (entity.position.rotation.y != 0.0f) {
                        return
                    }
                    entity.get(Movable::class).state = MOVING
                    val box = entity.get(BoundingBox::class)
                    val origin = Vector3(box.center.x, box.center.y, box.min.z)
                    val closest = (walls - entity).mapNotNull { other ->
                        val hit = rayResolver.intersectRayBounds(origin, Vector3(0, 0, -1), other)
                        hit
                    }.minByOrNull { it.dist2(origin) }
                    entity.get(Movable::class).x = 0f
                    entity.get(Movable::class).z = closest!!.z - origin.z
                    entity.add(ScriptComponent(
                        script = { moveObj(entity) }
                    ))
                } else if (input.isKeyJustPressed(Key.ARROW_UP)) {
                    if (entity.position.rotation.y == 0.0f) {
                        return
                    }
                    entity.get(Movable::class).state = MOVING
                    val box = entity.get(BoundingBox::class)
                    val origin = Vector3(box.min.x, box.center.y, box.center.z)
                    val closest = (walls - entity).mapNotNull { other ->
                        val hit = rayResolver.intersectRayBounds(origin, Vector3(-1, 0, 0), other)
                        hit
                    }.minByOrNull { it.dist2(origin) }
                    entity.get(Movable::class).x = closest!!.x - origin.x
                    entity.get(Movable::class).z = 0f
                    entity.add(ScriptComponent(
                        script = { moveObj(entity) }
                    ))
                } else if (input.isKeyJustPressed(Key.ARROW_DOWN)) {
                    if (entity.position.rotation.y == 0.0f) {
                        return
                    }
                    entity.get(Movable::class).state = MOVING
                    val box = entity.get(BoundingBox::class)
                    val origin = Vector3(box.max.x, box.center.y, box.center.z)
                    val closest = (walls - entity).mapNotNull { other ->
                        val hit = rayResolver.intersectRayBounds(origin, Vector3(1, 0, 0), other)
                        hit
                    }.minByOrNull { it.dist2(origin) }
                    entity.get(Movable::class).x = closest!!.x - origin.x
                    entity.get(Movable::class).z = 0f
                    entity.add(ScriptComponent(
                        script = { moveObj(entity) }
                    ))
                }
            }
        }
    }

    suspend fun ScriptContext.moveObj(entity: Entity) {
        val speed = 4f
        val m = entity.get(Movable::class)
        if (m.x == 0f) {
            var quantity = abs(m.z)
            while (quantity > 0f) {
                quantity -= speed * delta
                entity.position.addLocalTranslation(z = sign(m.z) * speed, delta = delta)
                yield()
            }
            entity.position.addLocalTranslation(z = quantity * sign(m.z), delta = delta)
            entity.get(Movable::class).state = SELECTED
        } else {
            var quantity = abs(m.x)
            while (quantity > 0f) {
                quantity -= speed * delta
                entity.position.addLocalTranslation(x = sign(m.x) * speed, delta = delta)
                yield()
            }
            entity.position.addLocalTranslation(z = quantity * sign(m.x), delta = delta)
            entity.get(Movable::class).state = SELECTED
        }
    }
}

class PlayerSystem : System(EntityQuery.of(Player::class)) {

    private val collider = AABBCollisionResolver()

    private val targets by interested(EntityQuery.of(Target::class))

    private val instructions by interested(EntityQuery.of(Instruction::class))

    var end = false
    override fun update(delta: Seconds, entity: Entity) {
        if(end) return
        
        if (collider.collide(entity, targets.first())) {
            end = true
            
            instructions.forEach {
                it.get(TextComponent::class).text.content = "  !! Congratulation !!"
            }
        }
    }
}

class MyGame(override val gameContext: GameContext) : Game {

    private val scene by gameContext.fileHandler.get<Scene>("rushhours.protobuf")

    private val font by gameContext.fileHandler.get<Font>("font")

    override fun createEntities(entityFactory: EntityFactory) {
        // Create all entities needed at startup
        // The scene is the node graph that can be updated in Blender
        scene.children.forEach { node ->
            // Create an entity using all information from this node (model, position, camera, ...)
            val entity = entityFactory.createFromNode(node, scene)

            if (node.name == "player") {
                entity.chidren.first { it.name == "smoke" }
                    .add(SmokeComponent())
                entity.add(Player())
            }


            if (node.name.startsWith("two") || node.name.startsWith("three") || node.name == "player") {
                entity.add(Movable())
                entity.add(Wall())
            } else if (node.name.startsWith("stop")) {
                entity.add(Wall())
            }
            if (node.name == "text.001") {
                entityFactory.createText(
                    WaveEffect(WriteText("")), font, node, scene
                ).also {
                    it.get(TextComponent::class).horizontalAlign = HorizontalAlignment.Center
                    it.get(TextComponent::class).lineWith = 5
                    it.add(Instruction())
                }
            }

            if (node.name == "target") {
                entity.add(Target())
            }
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        val model = scene.children.first { it.name == "Icosphere" }
        // Create all systems used by the game
        return listOf(SmokeSystem(model, scene), SmoleParticuleSystem(), MovableSystem(), PlayerSystem())
    }
}
