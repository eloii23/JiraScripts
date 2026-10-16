import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.history.ChangeItemBean
import com.atlassian.jira.config.StatusCategoryManager
import com.atlassian.jira.config.StatusManager

def changeHistoryManager = ComponentAccessor.getChangeHistoryManager()
StatusManager statusManager = ComponentAccessor.getComponentOfType(StatusManager.class)
StatusCategoryManager statusCategoryManager = ComponentAccessor.getComponentOfType(StatusCategoryManager.class)

def html_table = "<table class='aui'><thead><tr><th>Status</th><th>Time</th></tr></thead><tbody>"

def status1Time
def status2Time

def statusCategory

def changeItems = changeHistoryManager.getChangeItemsForField(issue, "status")

if (changeItems.size() < 1){ //si la peticion esta en estado inicial, no hay historico
    status1Time = System.currentTimeMillis() - issue.getCreated().getTime()
    statusCategory = colorCategory(issue.getStatus().getStatusCategory().getId())
    
    String timeInStatus = formatTime(((status1Time / 1000) as long ?: 0L))
    
    html_table = html_table + "<tr><td><span class='aui-lozenge aui-lozenge"+statusCategory+"'>" + issue.getStatus().getName() + "</td><td>" + timeInStatus + "</td></tr>"
} else { //Si hay historico es porque se ha ejecutado como minimo una transicion
    for (int i = 0; i < changeItems.size(); i++) {
        ChangeItemBean item = changeItems[i]
        ChangeItemBean nextItem = changeItems[i+1]

        if (i == 0){//Logica para obtener el tiempo en estado inicial
            status1Time = item.created.getTime() - issue.getCreated().getTime()
            statusCategory = colorCategory(statusManager.getStatus(item.getFrom()).getStatusCategory().getId())
            
            String timeInStatus1 = formatTime(((status1Time / 1000) as long ?: 0L))
            
            html_table = html_table + "<tr><td><span data-status class='aui-lozenge aui-lozenge-"+statusCategory+"'>" + item.getFromString() + "</td><td>" + timeInStatus1+ "</td></tr>"
            
            if (changeItems.size() > 1) {//el tiempo final va en funcion de si hay mas registros en el historico o solo hay 1
                status2Time = nextItem.created.getTime() - item.created.getTime()
            } else {
                status2Time = System.currentTimeMillis() - item.created.getTime()
            }
            
            String timeInStatus2 = formatTime(((status2Time / 1000) as long ?: 0L))
            
            html_table = html_table + "<tr><td><span class='aui-lozenge'>" + item.getToString() + "</td><td>" + timeInStatus2 + "</td></tr>"
        } else if (i == changeItems.size() -1){//si es el ultimo estado, el tiempo final es el actual
        	status1Time = System.currentTimeMillis() - item.created.getTime()
            statusCategory = colorCategory(statusManager.getStatus(item.getTo()).getStatusCategory().getId())
            
            String timeInStatus = formatTime(((status1Time / 1000) as long ?: 0L))
            
            html_table = html_table + "<tr><td><span class='aui-lozenge aui-lozenge-"+statusCategory+"'>" + item.getToString() + "</td><td>" + timeInStatus + "</td></tr>"
        } else {//para los estados intermedios se compara una entrada del historico con la siguiente
            status1Time = nextItem.created.getTime() - item.created.getTime()
            statusCategory = colorCategory(statusManager.getStatus(item.getTo()).getStatusCategory().getId())
            
            String timeInStatus = formatTime(((status1Time / 1000) as long ?: 0L))
            
            html_table = html_table + "<tr><td><span class='aui-lozenge aui-lozenge-"+statusCategory+"'>" + item.getToString() + "</td><td>" + timeInStatus + "</td></tr>"
        }
    }
}

html_table = html_table + "</tbody></table>"

String colorCategory(Long id){
    String lozenge
    switch (id){
        case 2:
            return 'default'
        case 3:
            return 'success'
        case 4:
            return 'inprogress'
    }
}

String formatTime(Long timeInSeconds) {
    def hoursRest = (timeInSeconds/ (3600))
    def hours = hoursRest as Integer
    def minutesRest = ((timeInSeconds % 3600)/60)
    def minutes = minutesRest as Integer
    def secondsRest = ((timeInSeconds % 3600)%60)
    def seconds = secondsRest as Integer
    String valueToShow = " " + (hours as Integer).toString() +"h "+ (minutes as Integer).toString() + "m " + (seconds as Integer).toString() + "s"
    
    return valueToShow
}

return html_table
