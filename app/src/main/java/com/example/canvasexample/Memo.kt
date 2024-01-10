package com.example.canvasexample

/**
 * todo: 이미 연결된 노드끼리 또 다시 엣지를 생성하려고 할때 어떻게 할것인가
 * todo: Node move는 long click을 trigger로 해야할 수도 있겠다.
 * todo: 노드를 확대 했을때 화면의 중심에 위치하도록 하자.
 * todo: Scale 변경이 노드와 엣지의 recomposition을 trigger하지 않는다.
 *       코드를 보면 당연하다. mutableStateListOf가 recomposition을 trigger하지 않기 때문이다.
 *       그리고 scale 변경시에 버벅거림이 발생해. 이는 내 생각에는 scale을 변경 하는 것으로 노드와 엣지
 *       모두에 recomposition이 발생하기 때문인것 같다.
 *       따라서, operate에서 바꾼것 처럼 특정 scale 값이 되면 node의 크기를 변경 시켜주는 recomposition을
 *       trigger 시켜 주면 어떨까 한다.
 *
 * todo: 사이즈에 애니메이션이 등록되어 있지 않아서 그런것 같아.
 *
 * 현재 노드를 통해서 동작 해야 하는 것
 *      1. 확대, 축소
 *      2. 노드 이동
 *      3. 노드 색 바꾸는 것
 *
 * */