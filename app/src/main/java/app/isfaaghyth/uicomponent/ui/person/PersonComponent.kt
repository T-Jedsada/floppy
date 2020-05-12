package app.isfaaghyth.uicomponent.ui.person

import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import app.isfaaghyth.uicomponent.component.EventBusFactory
import app.isfaaghyth.uicomponent.component.UIComponent
import app.isfaaghyth.uicomponent.dataview.Person
import app.isfaaghyth.uicomponent.dispatchers.DispatcherProvider
import app.isfaaghyth.uicomponent.state.ScreenStateEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PersonComponent(
    container: ViewGroup,
    private val bus: EventBusFactory,
    coroutineScope: CoroutineScope,
    dispatcher: DispatcherProvider
): UIComponent<PersonInteractionEvent>, CoroutineScope by coroutineScope, PersonUIView.Listener {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val uiView = initView(container)

    private fun initView(container: ViewGroup): PersonUIView {
        return PersonUIView(container, this)
    }

    init {
        launch(dispatcher.immediate()) {
            bus.getSafeManagedFlow(ScreenStateEvent::class.java)
                .collect {
                    when (it) {
                        ScreenStateEvent.Init -> uiView.hide()
                        is ScreenStateEvent.SetPersonInfo -> {
                            setPersonInfo(it.person)
                        }
                    }
                }
        }
    }

    private fun setPersonInfo(person: Person) {
        uiView.setPersonInfo(person)
        uiView.show()
    }

    override fun onPersonInfoClicked(person: Person) {
        launch {
            bus.emit(
                PersonInteractionEvent::class.java,
                PersonInteractionEvent.PersonInfoClicked(person)
            )
        }
    }

    override fun interactionEvents(): Flow<PersonInteractionEvent> {
        return bus.getSafeManagedFlow(PersonInteractionEvent::class.java)
    }

    override fun containerId(): Int {
        return uiView.containerId
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        uiView.onDestroy()
    }

    companion object {
        fun init(
            container: ViewGroup,
            coroutineScope: CoroutineScope,
            lifecycleOwner: LifecycleOwner,
            dispatcher: DispatcherProvider,
            onAction: (event: PersonInteractionEvent) -> Unit
        ): UIComponent<PersonInteractionEvent> {
            val pinnedComponent = PersonComponent(
                container,
                EventBusFactory.get(lifecycleOwner),
                coroutineScope,
                dispatcher
            ).also(lifecycleOwner.lifecycle::addObserver)

            coroutineScope.launch {
                pinnedComponent.interactionEvents()
                    .collect { onAction(it) }
            }

            return pinnedComponent
        }
    }

}