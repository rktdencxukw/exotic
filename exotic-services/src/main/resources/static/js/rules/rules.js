$(function() {
    $('#timezone').val(new Date().getTimezone())

    let d = moment().format('YYYY-MM-DD')
    $('#startTime').datetimepicker({
        inline: false,
//        format: 'Y-m-dTH:i',
//        formatDate: 'Y-m-d'
//        format: 'yyyy-MM-ddTHH:mm:ssZ'
        format: 'Y-m-dTH:i:sZ'
    }).val(d + 'T00:00:00Z')

    let deadTimeJQ = $('#deadTime')
    // let deadTime = deadTimeJQ.val()
    // deadTime = moment().format(deadTime, 'YYYY-MM-DD')
    deadTimeJQ.datetimepicker({
        inline: false,
        timepicker: false,
//        format: 'Y-m-d H:i',
        format: 'Y-m-dTH:i:sZ'
//        format: 'yyyy-MM-ddTHH:mm:ssZ'
//        formatDate: 'Y-m-d'
    })

    $('.cronExpression').hide()

    $('#period').change(function() {
        let value = $('#period').val()
        console.log(value)
        if (value === 'PT-1H') {
            $('.cronExpression').show()
        } else {
            $('.cronExpression').hide()
        }
    })

    $('#type').change(function() {
        let value = $('#type').val()
        if (value === 'Entity') {
            $('#outLinkSelectorGroup').hide()
        } else {
            $('#outLinkSelectorGroup').show()
        }
    })

    $('#btnTestRun').click(function(event) {
        //stop submit the form event. Do this manually using ajax post function
        event.preventDefault();

        var form = {}
        $('#ruleForm').serializeArray().forEach(function(item) {
            form[item.name] = item.value
        })

        $("#btnTestRun").prop("disabled", true);

        $.ajax({
            type: "POST",
            contentType: "application/json",
            url: "/exotic/crawl/rules/test_run",
            data: JSON.stringify(form),
            dataType: 'json',
            cache: false,
            timeout: 600000,
            success: function (data) {
//                var json = "<h4>Ajax Response</h4><pre>"
//                    + JSON.stringify(data, null, 4) + "</pre>";
//                $('#feedback').html(json);

                console.log("SUCCESS : ", data);
//                $("#btn-login").prop("disabled", false);
            },
            error: function (e) {
//                var json = "<h4>Ajax Response Error</h4><pre>"
//                    + e.responseText + "</pre>";
//                $('#feedback').html(json);

                console.log("ERROR : ", e);
//                $("#btn-login").prop("disabled", false);

            }
        });
    })

    $('#cronExpressionBuilder').cronBuilder({
        onChange: function(expression) {
            $('#cronExpression').val(expression);
        }
    });

    $('input[type=submit]').click(function(e) {
        correctDateTime('#startTime')
        correctDateTime('#deadTime')
    })
});
