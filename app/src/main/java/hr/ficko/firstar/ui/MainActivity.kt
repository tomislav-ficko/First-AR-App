package hr.ficko.firstar.ui

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import hr.ficko.firstar.R
import hr.ficko.firstar.models.Model
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

private const val BOTTOM_SHEET_PEEK_HEIGHT = 50f
private const val DOUBLE_TAP_TOLERANCE_MS = 1000L

class MainActivity : AppCompatActivity() {

    private lateinit var modelAdapter: ModelAdapter
    private lateinit var selectedModel: Model
    lateinit var arFragment: ArFragment
    private val viewNodes = mutableListOf<Node>()

    private val models = mutableListOf(
        Model(R.drawable.chair, "Chair", R.raw.chair),
        Model(R.drawable.oven, "Oven", R.raw.oven),
        Model(R.drawable.piano, "Piano", R.raw.piano),
        Model(R.drawable.table, "Table", R.raw.table)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = fragment as ArFragment
        modelAdapter = ModelAdapter(models)
        setupBottomSheet()
        observeLiveData()
        setupRecyclerView()
        setupDoubleTapArPlaneListener()
        setupAutomaticModelButtonRotation()
    }

    private fun setupBottomSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            BOTTOM_SHEET_PEEK_HEIGHT,
            resources.displayMetrics
        ).toInt()
        bottomSheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    bottomSheet.bringToFront()
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
    }

    private fun setupRecyclerView() {
        rvModels.apply {
            layoutManager =
                LinearLayoutManager(
                    this@MainActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
            adapter = modelAdapter
        }
    }

    private fun setupDoubleTapArPlaneListener() {
        var firstTapTime = 0L
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            when {
                firstTapTime == 0L -> {
                    firstTapTime = System.currentTimeMillis()
                }
                isValidDoubleTap(firstTapTime, System.currentTimeMillis()) -> {
                    firstTapTime = 0L
                    loadModel { modelRenderable, viewRenderable ->
                        val anchor = hitResult.createAnchor()
                        addNodeToScene(anchor, modelRenderable, viewRenderable)
                    }
                }
                else -> {
                    firstTapTime = System.currentTimeMillis()
                }
            }
        }
    }

    private fun setupAutomaticModelButtonRotation() {
        getCurrentScene().addOnUpdateListener {
            // Called in each frame, 60 or more times per second
            rotateViewNodesTowardsUser()
        }
    }

    private fun observeLiveData() {
        val dataObserver = defineDataObserver()
        modelAdapter.selectedModelLiveData.observe(this, dataObserver)
    }

    private fun defineDataObserver() = Observer<Model> { data ->
        data?.let {
            selectedModel = it
            val newTitle = "Models (${it.title})"
            tvModel.text = newTitle
        }
    }

    // ViewRenderable is a 3D representation of an XML layout, which can be displayed in the AR scene
    // (e.g. displaying buttons above objects placed in the scene)
    // Note: 250dp is equal to one meter
    private fun loadModel(callback: (ModelRenderable, ViewRenderable) -> Unit) {
        val modelRenderable = ModelRenderable.builder()
            .setSource(this, selectedModel.modelResourceId)
            .build()
        val viewRenderable = ViewRenderable.builder()
            .setView(this, createDeleteButton())
            .build()

        CompletableFuture.allOf(modelRenderable, viewRenderable)
            .thenAccept {
                // The get() methods may be called only inside the thenAccept{}
                // block because inside it  we know the renderable is fully loaded
                callback(modelRenderable.get(), viewRenderable.get())
            }.exceptionally {
                showErrorToast(it)
            }
    }

    // It can also be accomplished by inflating an XML layout
    private fun createDeleteButton(): Button {
        return Button(this).apply {
            text = "Delete"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
        }
    }

    private fun showErrorToast(error: Throwable?): Void? {
        Toast.makeText(this, "Error loading model: ${error!!}", Toast.LENGTH_LONG)
            .show()
        return null
    }

    private fun addNodeToScene(
        anchor: Anchor,
        modelRenderable: ModelRenderable,
        viewRenderable: ViewRenderable
    ) {
        val anchorNode = AnchorNode(anchor)
        val modelNode = TransformableNode(arFragment.transformationSystem).apply {
            renderable = modelRenderable
            setParent(anchorNode)
            getCurrentScene().addChild(anchorNode)
            select()
        }
        val viewNode = Node().apply {
            renderable = initiallyNotVisible()
            setParent(modelNode)
            setButtonPosition(this, modelNode)
            removeModelFromSceneIfButtonClicked(viewRenderable, this, anchorNode)
        }
        viewNodes.add(viewNode)

        modelNode.setOnTapListener { _, _ ->
            if (modelIsNotBeingMovedJustTapped(modelNode)) {
                toggleButton(viewNode, viewRenderable)
            }
        }
    }

    private fun toggleButton(
        viewNode: Node,
        viewRenderable: ViewRenderable
    ) {
        if (viewNode.renderable == null) {
            viewNode.renderable = viewRenderable
        } else {
            viewNode.renderable = null
        }
    }

    private fun setButtonPosition(viewNode: Node, modelNode: TransformableNode) {
        val collisionBox = modelNode.renderable?.collisionShape as Box
        val modelHeight = collisionBox.size.y
        // We are tying the position of the Delete button to the local position of the model,
        // so that we don't have to deal with calculating the world position of the model.
        // The position of the button will be right above the model (X=0, Y=modelHeight, Z=0)
        viewNode.localPosition = Vector3(0f, modelHeight, 0f)
    }

    private fun removeModelFromSceneIfButtonClicked(
        viewRenderable: ViewRenderable,
        viewNode: Node,
        anchorNode: AnchorNode
    ) {
        (viewRenderable.view as Button).setOnClickListener {
            // Since we are deleting the parent node, the children will also be deleted
            getCurrentScene().removeChild(anchorNode)
            viewNodes.remove(viewNode)
        }
    }

    private fun rotateViewNodesTowardsUser() {
        for (node in viewNodes) {
            rotateIfVisible(node)
        }
    }

    private fun rotateIfVisible(node: Node) {
        node.renderable?.let {
            val cameraPosition = getCurrentScene().camera.worldPosition
            val viewNodePosition = node.worldPosition
            val direction = Vector3.subtract(cameraPosition, viewNodePosition)
            node.worldRotation = Quaternion.lookRotation(direction, Vector3.up())
        }
    }

    private fun getCurrentScene() = arFragment.arSceneView.scene
    private fun initiallyNotVisible() = null
    private fun modelIsNotBeingMovedJustTapped(modelNode: TransformableNode) =
        !modelNode.isTransforming

    private fun isValidDoubleTap(firstTapTime: Long, secondTapTime: Long) =
        secondTapTime - firstTapTime < DOUBLE_TAP_TOLERANCE_MS
}
