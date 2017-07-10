const TRANSITION_DURATION = 200; //millis
const DOT_RADIUS_SMALL = 3.5;
const DOT_RADIUS_LARGE = 10;
const BACKGROUND_COLOR = '#D9F0FF';
const CONSUMED_COLOR = '#00A79D';
const PRODUCED_COLOR = 'black';
const PRODUCED_CIRCLE_COLOR = '#5FB0DF';
const CONSUMED_CIRCLE_COLOR = '#88E0F9';

const margin = {top: 30, right: 20, bottom: 50, left: 70};
const width = 1000 - margin.left - margin.right;
const height = 500 - margin.top - margin.bottom;

const now = new Date();
const ONE_DAY = 24 * 60 * 60 * 1000;

const svg = d3.select(".panel-body")
    .insert("svg", 'table')
    .attr("class", 'bg-accent-6')
    .attr("style", 'width: 100%')
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
    .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

svg.append("text")
    .attr("x", width / 2)
    .attr("y", -10)
    .attr("text-anchor", "middle")
    .style("font-size", "16px")
    .style("text-decoration", "underline")
    .text("Test Log Chart");

svg.append("text")
    .attr("x", width / 2)
    .attr("y", height + margin.top + 10)
    .style("text-anchor", "middle")
    .text("Time");
svg.append("text")
    .attr("x", -height / 2)
    .attr("y", -margin.left / 2 - 10)
    .style("text-anchor", "middle")
    .style("transform", "rotate(-90deg)")
    .text("Log Numbers");

const legendRectSize = 18;
const legendSpacing = 4;
const legend = svg.selectAll('.legend')
    .data([{name: "Logs Produced", color: PRODUCED_COLOR}, {name: "Logs Consumed", color: CONSUMED_COLOR}])
    .enter().append('g')
    .attr('class', 'legend')
    .attr('transform', function (d, i) {
        const height = legendRectSize + legendSpacing;
        const offset = height;
        const x = width - margin.right - (-2 * legendRectSize);
        const y = margin.top + (i * height - offset);
        return 'translate(' + x + ',' + y + ')';
    });
legend.append('rect')
    .attr('width', legendRectSize)
    .attr('height', legendRectSize)
    .style('fill', d => d.color)
    .style('stroke', d => d.color);
legend.append('text')
    .attr('x', legendRectSize + legendSpacing)
    .attr('y', legendRectSize - legendSpacing)
    .text(d => d.name);

// Get the data
d3.json("tests", function (error, data) {
    if (error) throw error;

    data = data.filter(d => d.logsConsumed >= 0);

    data.forEach(function (d) {
        d.startTime = new Date(d.startTime * 1000);
        d.logsProduced = +d.logsProduced;
        d.logsConsumed = +d.logsConsumed;
    });

    const x = d3.scaleTime()
        .range([0, width])
        .domain([now - ONE_DAY, now]);
    const y = d3.scaleLinear()
        .range([height, 0])
        .domain([0, d3.max(data, d => Math.max(d.logsProduced, d.logsConsumed))]);

    // Add the X Axis
    svg.append("g")
        .attr("transform", "translate(0," + height + ")")
        .call(d3.axisBottom(x).ticks(24).tickFormat(d3.timeFormat("%H:%M")));

    // Add the Y Axis
    svg.append("g")
        .call(d3.axisLeft(y));

    // define the line
    const valueline = yProp => d3.line()
        .x(d => x(d.startTime))
        .y(d => y(d[yProp]));

    const renderLine = function (root, lineColor, prop) {
        root.append("path")
            .data([data])
            .attr("class", "line")
            .style("fill", "none")
            .style("stroke", lineColor)
            .attr("d", valueline(prop));
    };

    renderLine(svg, PRODUCED_COLOR, "logsProduced");
    renderLine(svg, CONSUMED_COLOR, "logsConsumed");

    const tooltip = d3.select(".panel-body").append("div")
        .attr("class", "tooltip bg-neutral-6")
        .style("opacity", 0);

    const renderDots = function (root, points, dotColor, prop) {
        root.selectAll("dot")
            .data(points)
            .enter()
            .append("circle")
            .attr("stroke", dotColor)
            .attr("fill", BACKGROUND_COLOR)
            .attr("r", DOT_RADIUS_SMALL)
            .attr("cx", d => x(d.startTime))
            .attr("cy", d => y(d[prop]))
            .on("mouseover", function (d) {
                tooltip.transition()
                    .duration(TRANSITION_DURATION)
                    .style("opacity", .9);
                tooltip.html(d3.timeFormat("%d-%b-%y")(d.startTime) + "<br/>" + d[prop])
                    .style("left", (d3.event.pageX) + "px")
                    .style("top", (d3.event.pageY - 28) + "px");
                transitionToSizeAndColor(this, DOT_RADIUS_LARGE, dotColor);
            })
            .on("mouseout", function () {
                tooltip.transition()
                    .duration(TRANSITION_DURATION)
                    .style("opacity", 0);
                transitionToSizeAndColor(this, DOT_RADIUS_SMALL, BACKGROUND_COLOR);
            });
    };

    renderDots(svg, data, PRODUCED_CIRCLE_COLOR, "logsProduced");
    renderDots(svg, data, CONSUMED_CIRCLE_COLOR, "logsConsumed");
});

function transitionToSizeAndColor(item, size, color) {
    d3.select(item)
        .transition()
        .duration(TRANSITION_DURATION)
        .attr('r', size)
        .attr('fill', color)
}
