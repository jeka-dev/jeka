import react.child
import react.dom.render
import kotlinx.browser.document

fun main() {
    render(document.getElementById("root")) {
        child(App)
    }
}