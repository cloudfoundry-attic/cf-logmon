const TRANSITION_DURATION = 200; //millis
const DOT_RADIUS_SMALL = 3.5;
const DOT_RADIUS_LARGE = 10;
const BACKGROUND_COLOR = '#D9F0FF';
const CONSUMED_COLOR = '#00A79D';
const PRODUCED_COLOR = 'black';
const PRODUCED_CIRCLE_COLOR = '#5FB0DF';
const CONSUMED_CIRCLE_COLOR = '#88E0F9';

const now = new Date();
const ONE_DAY = 24 * 60 * 60 * 1000;

const margin = {top: 40, right: 20, bottom: 50, left: 30};
const width = document.querySelector('.panel-body.graph').clientWidth - margin.left - margin.right;
const height = document.querySelector('.panel-body.graph').clientHeight - margin.top - margin.bottom;

// Get the data
function retrieveDataAndGraphIt() {
    d3.json("tests", function (error, data) {
        d3.select('.panel-body.graph').html("");
        if (error) throw error;
        if (data.length === 0) {
            d3.select(".panel-body.graph")
                .style('display', 'flex')
                .style('justify-content', 'center')
                .style('align-items', 'center')
                .text("No data available just yet.");
            return;
        }
        const svg = d3.select(".panel-body.graph")
            .insert("svg", 'table')
            .attr("style", 'width: 100%')
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
            .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ") scale(0.975)");

        const renderText = (text, root, x, y) => root.append('text').attr('x', x).attr('y', y).style('text-anchor', 'middle').text(text);

        renderText('Time', svg, width / 2, height + margin.top + 10);
        renderText('Number of Logs', svg, -height / 2, -margin.left / 2 + 5).style('transform', 'rotate(-90deg)');

        const legendRectSize = 18;
        const legendSpacing = 4;
        const legend = svg.selectAll('.legend')
            .data([{name: "Logs Produced", color: PRODUCED_COLOR}, {name: "Logs Consumed", color: CONSUMED_COLOR}])
            .enter().append('g')
            .attr('class', 'legend')
            .attr('transform', function (d, i) {
                const x = -6 + i * 104;
                const y = -margin.top;
                return `translate(${x},${y})`;
            });
        legend.append('rect')
            .attr('transform', 'translate(0, 8)')
            .attr('width', legendRectSize)
            .attr('height', 2)
            .style('fill', d => d.color)
            .style('stroke', d => d.color);
        legend.append('text')
            .attr('x', legendRectSize + legendSpacing)
            .attr('y', legendRectSize - legendSpacing)
            .style('font-size', '12px')
            .text(d => d.name);

        data = data.filter(d => d.logsConsumed >= 0);

        data.forEach(function (d) {
            d.startTime = new Date(d.startTime * 1000);
            d.logsProduced = +d.logsProduced;
            d.logsConsumed = +d.logsConsumed;
        });

        const x = d3.scaleTime()
            .range([50, width])
            .domain(d3.extent(data, d => d.startTime));
        const maxY = d3.max(data, d => Math.max(d.logsProduced, d.logsConsumed));
        const y = d3.scaleLinear()
            .range([height, 0])
            .domain([
                Math.max(d3.min(data, d => Math.min(d.logsProduced, d.logsConsumed)) - (maxY * 0.2), 0),
                maxY * 1.2
            ]);

        // Add the X Axis
        const xAxis = svg.append("g")
            .attr("transform", "translate(0," + height + ")")
            .call(d3.axisBottom(x));
        xAxis.select('.domain').remove();

        // Add the Y Axis
        const yAxis = svg.append("g")
            .call(d3.axisRight(y).tickSize(width + 50));
        yAxis.select('.domain').remove();
        yAxis.selectAll(".tick line").attr("stroke", "#EEEEEE").attr("stroke-width", "2");
        yAxis.selectAll(".tick text").attr("x", 4).attr("dy", -4);

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

        const tooltip = d3.select(".panel-body.graph").append("div")
            .attr("class", "tooltip bg-neutral-6")
            .style("opacity", 0);

        const renderDots = function (root, points, dotColor, prop) {
            root.selectAll("dot")
                .data(points.filter((p, i) => i === 0 || points[i][prop] !== points[i-1][prop]))
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
                        .style("left", (d3.event.offsetX) + "px")
                        .style("top", (d3.event.offsetY - 28) + "px");
                    transitionToSizeAndColor(this, DOT_RADIUS_LARGE, dotColor);
                })
                .on("mouseout", function () {
                    tooltip.transition()
                        .duration(TRANSITION_DURATION)
                        .style("opacity", 0);
                    transitionToSizeAndColor(this, DOT_RADIUS_SMALL, BACKGROUND_COLOR);
                });
        };

        renderDots(svg, data, CONSUMED_CIRCLE_COLOR, "logsConsumed");
    });
}

function transitionToSizeAndColor(item, size, color) {
    d3.select(item)
        .transition()
        .duration(TRANSITION_DURATION)
        .attr('r', size)
        .attr('fill', color)
}

retrieveDataAndGraphIt();
setInterval(retrieveDataAndGraphIt, 5000);
